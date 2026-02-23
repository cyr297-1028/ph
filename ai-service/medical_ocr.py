from fastapi import APIRouter, File, UploadFile
import shutil
import os
import sys
import logging
import numpy as np

# 创建一个路由模块
router = APIRouter()

# --- PaddleOCR 初始化 ---
try:
    from paddleocr import PaddleOCR
    logging.getLogger("ppocr").setLevel(logging.WARNING)
    print("⏳ 正在加载 PaddleOCR 模型...")
    ocr_engine = PaddleOCR(use_angle_cls=True, lang="ch")
    print("✅ PaddleOCR 加载成功！")
except ImportError:
    print("❌ 错误: 未找到 paddleocr 模块。")
    sys.exit(1)

# --- 辅助函数 ---
def parse_paddlex_result(result):
    boxes = []
    if not result: return []
    data = result[0] if isinstance(result, list) and len(result) > 0 else result
    if not isinstance(data, dict): return []
    rec_texts = data.get('rec_texts', [])
    dt_polys = data.get('dt_polys') or data.get('rec_boxes')
    
    if not dt_polys or len(dt_polys) != len(rec_texts):
        return [{"text": t, "x": 0, "center_y": 0, "height": 0} for t in rec_texts]
        
    for i, text in enumerate(rec_texts):
        if not text: continue
        try:
            points = np.array(dt_polys[i]).reshape(-1, 2)
            min_x, min_y = np.min(points, axis=0)
            max_x, max_y = np.max(points, axis=0)
            boxes.append({
                "text": text, "x": float(min_x),
                "center_y": float((min_y + max_y) / 2),
                "height": float(max_y - min_y)
            })
        except: boxes.append({"text": text, "x": 0, "center_y": 0, "height": 0})
    return boxes

def merge_lines(boxes):
    if not boxes: return []
    if all(b['center_y'] == 0 for b in boxes): return [b['text'] for b in boxes]
    boxes.sort(key=lambda b: b['center_y'])
    lines, current_line = [], [boxes[0]]
    for i in range(1, len(boxes)):
        box, last = boxes[i], current_line[-1]
        avg_h = (box['height'] + last['height']) / 2 or 10
        if abs(box['center_y'] - last['center_y']) < (avg_h * 0.6):
            current_line.append(box)
        else:
            lines.append(current_line)
            current_line = [box]
    lines.append(current_line)
    return [" ".join([b['text'] for b in sorted(line, key=lambda x: x['x'])]) for line in lines]

# --- 接口定义 ---
@router.post("/ocr/medical_report")
async def ocr_predict(file: UploadFile = File(...)):
    save_dir = "./temp_medical"
    if not os.path.exists(save_dir): os.makedirs(save_dir)
    file_path = os.path.abspath(os.path.join(save_dir, file.filename))
    
    try:
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
        
        result = ocr_engine.ocr(file_path)
        boxes = parse_paddlex_result(result)
        lines = merge_lines(boxes)
        final_text = "\n".join(lines)

        if os.path.exists(file_path): os.remove(file_path)

        return {"code": 200, "msg": "识别成功", "data": final_text}
    except Exception as e:
        if os.path.exists(file_path): os.remove(file_path)
        return {"code": 500, "msg": f"OCR 失败: {str(e)}", "data": ""}