import uvicorn
from fastapi import FastAPI, File, UploadFile
import shutil
import os
import sys
import logging

# 尝试导入 PaddleOCR
try:
    from paddleocr import PaddleOCR
except ImportError:
    print("❌ 错误: 未找到 paddleocr 模块。请先运行: pip install paddleocr paddlepaddle")
    sys.exit(1)

# 抑制调试日志
logging.getLogger("ppocr").setLevel(logging.WARNING)

app = FastAPI()

# ==========================================
# 1. 初始化 PaddleOCR
# ==========================================
print("⏳ 正在加载 PaddleOCR 模型...")

try:
    # 【核心调整】简化初始化参数，使用默认值以确保最稳定的兼容性
    # 移除复杂的阈值参数，防止参数名版本冲突导致 pipeline 异常
    ocr_engine = PaddleOCR(
        lang="ch",           # 中文模式
        use_angle_cls=True   # 开启方向检测 (大多数版本兼容此参数)
    )
    print("✅ PaddleOCR 加载成功！")
except Exception as e:
    print(f"⚠️ 默认参数加载失败: {e}，尝试使用备用参数...")
    try:
        # 备用：针对新版 PaddleOCR 的参数
        ocr_engine = PaddleOCR(
            lang="ch", 
            use_textline_orientation=True
        )
        print("✅ PaddleOCR (新版参数) 加载成功！")
    except Exception as e2:
        print(f"❌ PaddleOCR 加载彻底失败: {e2}")
        sys.exit(1)

def parse_ocr_result_to_lines(ocr_result):
    """
    核心逻辑：将 OCR 返回的散乱方块，根据 Y 坐标合并成人类可读的“行”
    """
    if ocr_result is None or len(ocr_result) == 0 or ocr_result[0] is None:
        return []

    boxes = []
    raw_texts = [] # 备用：如果获取不到坐标，就只存文本

    for line in ocr_result[0]:
        # line 结构可能异常，必须防御性检查
        # 预期: [ [[x,y]...], ('text', 0.9) ]
        
        points = line[0]
        text_info = line[1]
        
        text = ""
        score = 0.0
        
        # 解析文本和分数
        if isinstance(text_info, (list, tuple)):
            text = text_info[0]
            score = text_info[1] if len(text_info) > 1 else 1.0
        elif isinstance(text_info, str):
            text = text_info
            score = 1.0
            
        # ⚠️ 【核心修复】检查 points 是否真的是坐标列表
        if not isinstance(points, list):
            # 如果 points 不是列表（比如是字符串），说明没有坐标信息
            # 这种情况无法进行行合并，只能存入原始列表
            if score > 0.5:
                raw_texts.append(text)
            continue 

        # 只要置信度大于 0.3 就保留
        if score > 0.3:
            try:
                # 尝试解析坐标
                xs = [p[0] for p in points]
                ys = [p[1] for p in points]
                avg_y = sum(ys) / len(ys)
                min_x = min(xs)
                
                boxes.append({
                    "text": text,
                    "y": avg_y,
                    "x": min_x,
                    "h": max(ys) - min(ys)
                })
            except Exception:
                # 如果坐标解析失败，降级处理
                raw_texts.append(text)

    # 如果没有成功提取到任何带坐标的框，但有纯文本
    if not boxes and raw_texts:
        print("⚠️ 警告: 未检测到坐标信息，返回原始文本顺序")
        return raw_texts

    # 2. 按 Y 坐标排序
    boxes.sort(key=lambda b: b['y'])

    # 3. 智能合并同行的文字
    lines = []
    current_line = []
    
    for i, box in enumerate(boxes):
        if i == 0:
            current_line.append(box)
            continue
        
        last_box = current_line[-1]
        
        # 判断是否在同一行
        y_diff = abs(box['y'] - last_box['y'])
        height_threshold = max(box['h'], last_box['h']) * 0.6 
        
        if y_diff < height_threshold:
            current_line.append(box)
        else:
            current_line.sort(key=lambda b: b['x'])
            lines.append(current_line)
            current_line = [box]
    
    if current_line:
        current_line.sort(key=lambda b: b['x'])
        lines.append(current_line)

    # 4. 拼接文字
    final_lines = []
    for line_boxes in lines:
        line_text = " ".join([b['text'] for b in line_boxes])
        final_lines.append(line_text)
        
    return final_lines

@app.post("/ocr/medical_report")
async def ocr_predict(file: UploadFile = File(...)):
    save_dir = "../temp_uploads"
    if not os.path.exists(save_dir):
        os.makedirs(save_dir)
        
    file_path = os.path.abspath(os.path.join(save_dir, file.filename))
    print(f"📥 正在接收图片: {file.filename}")
    
    try:
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
        
        print(f"📂 图片已保存: {file_path}")
        print("🔍 开始识别...")
        
        # 【核心调用】强制 det=True (检测+识别)，确保返回坐标
        # cls=True (方向矫正)
        result = ocr_engine.ocr(file_path, det=True, cls=True)
        
        # 解析逻辑
        lines = parse_ocr_result_to_lines(result)
        
        # 拼接成最终文本
        final_text = "\n".join(lines)
        
        print(f"✅ 识别成功，提取到 {len(lines)} 行数据")
        
        try:
            os.remove(file_path)
        except:
            pass

        return {
            "code": 200, 
            "msg": "识别成功",
            "data": final_text 
        }

    except Exception as e:
        import traceback
        traceback.print_exc()
        print(f"❌ 处理异常: {str(e)}")
        # 发生错误时，返回空字符串而不是 500，防止前端报错卡死
        return {"code": 200, "msg": f"识别异常: {str(e)}", "data": ""}

if __name__ == '__main__':
    uvicorn.run(app, host="0.0.0.0", port=60061)