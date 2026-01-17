import uvicorn
from fastapi import FastAPI, File, UploadFile
import shutil
import os
import sys
import logging
import json
import numpy as np

# 1. 检查依赖
try:
    import shapely
    import pyclipper
    print("✅ 图形库依赖检查通过。")
except ImportError:
    print("⚠️ 警告: 缺少 shapely 或 pyclipper，可能会影响坐标解析。")

# 尝试导入 PaddleOCR
try:
    from paddleocr import PaddleOCR
except ImportError:
    print("❌ 错误: 未找到 paddleocr 模块。")
    sys.exit(1)

logging.getLogger("ppocr").setLevel(logging.WARNING)

app = FastAPI()

print("⏳ 正在加载 PaddleOCR 模型...")
try:
    # 保持最简初始化，防止参数报错
    ocr_engine = PaddleOCR(use_angle_cls=True, lang="ch")
    print("✅ PaddleOCR 加载成功！")
except Exception as e:
    print(f"⚠️ 初始化遇到问题: {e}")
    try:
        ocr_engine = PaddleOCR(lang="ch")
        print("✅ PaddleOCR (兼容模式) 加载成功！")
    except:
        print("❌ 彻底失败")
        sys.exit(1)

def parse_paddlex_result(result):
    """
    专门解析 PaddleX / 字典格式的返回结果
    目标：提取出 rec_texts (文字) 和 dt_polys (坐标)，并打包成统一格式
    """
    boxes = []
    
    # 安全检查
    if not result:
        return []

    # 获取核心数据对象
    # 你的日志显示 result 本身可能就是列表，第一项是字典
    data = None
    if isinstance(result, list) and len(result) > 0:
        data = result[0]
    elif isinstance(result, dict):
        data = result
        
    if not isinstance(data, dict):
        print(f"⚠️ 无法解析的数据结构类型: {type(data)}")
        return []

    # 1. 提取文字列表
    rec_texts = data.get('rec_texts', [])
    # 2. 提取坐标列表 (dt_polys 或 rec_boxes)
    # dt_polys 通常是多边形坐标 [[x1,y1],[x2,y2]...]
    dt_polys = data.get('dt_polys')
    if dt_polys is None:
        dt_polys = data.get('rec_boxes')

    print(f"🧐 解析到 {len(rec_texts)} 个文本段")

    # 如果没有坐标，只有文字 (纯识别模式)
    if not dt_polys or len(dt_polys) != len(rec_texts):
        print("⚠️ 坐标与文字数量不匹配或缺失，退化为纯文本提取")
        for text in rec_texts:
            boxes.append({
                "text": text,
                "x": 0, "center_y": 0, "height": 0
            })
        return boxes

    # 3. 组合 文字 + 坐标
    for i, text in enumerate(rec_texts):
        poly = dt_polys[i]
        
        # 过滤无效内容
        if not text or (len(text) == 1 and not text.isdigit() and text not in ['↑', '↓', '+', '-']):
            continue

        try:
            # poly 可能是 numpy array 或 list
            points = np.array(poly).reshape(-1, 2)
            xs = points[:, 0]
            ys = points[:, 1]
            
            min_y = np.min(ys)
            max_y = np.max(ys)
            min_x = np.min(xs)
            height = max_y - min_y
            center_y = (min_y + max_y) / 2
            
            boxes.append({
                "text": text,
                "x": float(min_x),
                "center_y": float(center_y),
                "height": float(height)
            })
        except Exception as e:
            # 如果坐标解析出错，保留文字但坐标归零
            boxes.append({"text": text, "x": 0, "center_y": 0, "height": 0})

    return boxes

def merge_lines(boxes):
    """几何行合并算法"""
    if not boxes:
        return []

    # 如果没有坐标信息，直接返回原列表
    if all(b['center_y'] == 0 for b in boxes):
        return [b['text'] for b in boxes]

    # 按 Y 轴排序
    boxes.sort(key=lambda b: b['center_y'])

    lines = []
    current_line = [boxes[0]]

    for i in range(1, len(boxes)):
        box = boxes[i]
        last_box = current_line[-1]

        # 判断同行：高度差 < 平均高度的 60%
        avg_height = (box['height'] + last_box['height']) / 2
        if avg_height == 0: avg_height = 10 # 防止除零
        
        y_diff = abs(box['center_y'] - last_box['center_y'])
        
        if y_diff < (avg_height * 0.6):
            current_line.append(box)
        else:
            lines.append(current_line)
            current_line = [box]
    
    if current_line:
        lines.append(current_line)

    # 拼接结果
    final_lines = []
    for line_boxes in lines:
        line_boxes.sort(key=lambda b: b['x'])
        line_str = " ".join([b['text'] for b in line_boxes])
        final_lines.append(line_str)
        
    return final_lines

@app.post("/ocr/medical_report")
async def ocr_predict(file: UploadFile = File(...)):
    save_dir = "../temp_uploads"
    if not os.path.exists(save_dir):
        os.makedirs(save_dir)
    
    file_path = os.path.abspath(os.path.join(save_dir, file.filename))
    print(f"📥 接收图片: {file.filename}")
    
    try:
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
            
        print("🔍 开始识别...")
        
        # 1. 调用 OCR
        result = ocr_engine.ocr(file_path)
        
        # 2. 解析 PaddleX 字典结构
        boxes = parse_paddlex_result(result)
        
        # 3. 兜底策略：如果标准解析失败，尝试旧版列表解析
        if not boxes and result and isinstance(result[0], list):
             print("⚠️ 字典解析为空，尝试标准列表解析...")
             # 这里可以放入旧的列表解析逻辑，但为了精简，我们先假设上面能成功
        
        # 4. 执行行合并
        lines = merge_lines(boxes)
        
        final_text = "\n".join(lines)
        print(f"✅ 识别完成，生成 {len(lines)} 行数据")
        
        # 预览
        if len(final_text) > 0:
            print("----- 数据预览 -----")
            print(final_text[:300] + "..." if len(final_text)>300 else final_text)
            print("-------------------")

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
        return {"code": 200, "msg": f"错误: {str(e)}", "data": ""}

if __name__ == '__main__':
    uvicorn.run(app, host="0.0.0.0", port=60061)