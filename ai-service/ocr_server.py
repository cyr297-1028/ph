import uvicorn
from fastapi import FastAPI, File, UploadFile
from paddleocr import PaddleOCR
import shutil
import os
import json

app = FastAPI()

# ==========================================
# 1. 初始化 PaddleOCR
# ==========================================
print("⏳ 正在加载 PaddleOCR 模型...")


ocr_engine = PaddleOCR(use_textline_orientation=True, lang="ch")

print("✅ PaddleOCR 加载成功！")

@app.post("/ocr/medical_report")
async def ocr_predict(file: UploadFile = File(...)):
    # 1. 确保临时目录存在
    save_dir = "temp_uploads"
    if not os.path.exists(save_dir):
        os.makedirs(save_dir)
        
    # 2. 保存文件 (使用 abspath 获取绝对路径，避免相对路径问题)
    file_path = os.path.abspath(os.path.join(save_dir, file.filename))
    
    print(f"📥 正在接收图片: {file.filename}")
    print(f"📂 本地保存路径: {file_path}")

    try:
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
        
        # 3. 检查文件是否存在且有大小
        if not os.path.exists(file_path) or os.path.getsize(file_path) == 0:
            return {"code": 400, "msg": "文件上传失败或文件为空"}

        # 4. 调用 OCR (核心修改点)
        print("🔍 开始识别...")
        
        # 注意：不再传 cls=True，因为初始化时已指定
        result = ocr_engine.ocr(file_path)
        
        # --- 调试打印 (看看 OCR 到底吐出了什么) ---
        print(f"🧐 OCR 原始返回数据: {result}") 

        # 5. 安全解析数据 (防止报错)
        ocr_items = []
        
        # 情况A: 结果为 None (常见于路径不对或完全无法读取)
        if result is None:
            print("⚠️ 警告: OCR 返回了 None")
            return {"code": 200, "msg": "未检测到任何文字(Result is None)", "data": {"items": []}}

        # 情况B: 结果是一个列表，但第一个元素是 None (常见于图片能读但没字)
        if len(result) > 0 and result[0] is None:
             print("⚠️ 警告: 图片中没有识别到文字")
             return {"code": 200, "msg": "未检测到任何文字", "data": {"items": []}}

        # 情况C: 正常解析
        # 这里的 result[0] 才是真正的行数据列表
        if result and len(result) > 0:
            for line in result[0]:
                # line 的结构通常是: [ [[x1,y1]...], ('文字', 0.99) ]
                points = line[0] 
                text_info = line[1] # ('文字', 0.99)
                
                text = text_info[0]
                confidence = text_info[1]
                
                # 计算坐标框
                xs = [p[0] for p in points]
                ys = [p[1] for p in points]
                
                ocr_items.append({
                    "text": text,
                    "box": {
                        "x": int(min(xs)),
                        "y": int(min(ys)),
                        "w": int(max(xs) - min(xs)),
                        "h": int(max(ys) - min(ys))
                    },
                    "score": float(confidence)
                })

        print(f"✅ 识别成功，共找到 {len(ocr_items)} 处文字")
        return {
            "code": 200, 
            "msg": "识别成功",
            "data": {
                "items": ocr_items,
                "image_path": file_path 
            }
        }

    except Exception as e:
        # 打印详细错误堆栈，方便排查
        import traceback
        traceback.print_exc()
        print(f"❌ OCR 处理过程中发生错误: {str(e)}")
        return {"code": 500, "msg": f"服务端内部错误: {str(e)}"}

if __name__ == '__main__':
    uvicorn.run(app, host="0.0.0.0", port=60061) 