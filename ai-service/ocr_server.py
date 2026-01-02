import uvicorn
from fastapi import FastAPI, File, UploadFile
from paddleocr import PPStructureV3
from paddlenlp import Taskflow
import os
import cv2
import shutil

app = FastAPI()

# 1. 初始化模型
try:
    print("⏳ 正在初始化 PaddleOCR V3 模型...")
    # image_orientation=True 可以自动纠正图片方向（解决图片歪了识别不到的问题）
    table_engine = PPStructureV3(lang='ch', image_orientation=True)
except Exception as e:
    print(f"⚠️ 模型初始化异常: {e}")
    table_engine = PPStructureV3()

# 初始化信息抽取模型
schema = ['姓名','项目名称','参考范围', '采样时间', '检测时间', '结果']
ie = Taskflow('information_extraction', schema=schema)

@app.post("/ocr/medical_report")
async def analyze_medical_report(file: UploadFile = File(...)):
    save_dir = "temp_uploads"
    if not os.path.exists(save_dir):
        os.makedirs(save_dir)
    
    file_path = os.path.join(save_dir, file.filename)

    try:
        # 保存文件
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
        print(f"✅ 文件已保存: {file_path}")

        # 调用识别
        print("⏳ 开始 OCR 识别 (V3模式)...")
        results = table_engine.predict(file_path)
        
        # ================= DEBUG 核心代码 =================
        # 强制遍历生成器，转为列表
        results_list = list(results)
        print(f"\n🔥🔥🔥 【DEBUG RAW DATA】 识别到的区域数量: {len(results_list)}")
        for idx, res in enumerate(results_list):
            r_type = getattr(res, 'type', 'unknown')
            print(f"   >>> 区域 {idx+1}: 类型={r_type}, 内容预览={str(res)[:50]}...")
        # =================================================

        report_tables = []
        full_text_list = []

        # 解析结果
        for res in results_list:
            res_dict = res.__dict__ if hasattr(res, '__dict__') else res
            
            # 1. 提取表格 (HTML)
            r_type = res_dict.get('type')
            if r_type == 'table':
                # 尝试多种路径获取 html
                html_content = None
                if isinstance(res_dict.get('res'), dict):
                    html_content = res_dict['res'].get('html')
                elif hasattr(res_dict.get('res'), 'html'):
                    html_content = res_dict['res'].html
                
                if html_content:
                    report_tables.append(html_content)

            # 2. 暴力提取所有文本 (递归)
            def extract_text_recursive(data):
                found_texts = []
                if isinstance(data, dict):
                    for k, v in data.items():
                        if k == 'text' and isinstance(v, str):
                            found_texts.append(v)
                        else:
                            found_texts.extend(extract_text_recursive(v))
                elif isinstance(data, list):
                    for item in data:
                        found_texts.extend(extract_text_recursive(item))
                elif hasattr(data, '__dict__'):
                     found_texts.extend(extract_text_recursive(data.__dict__))
                return found_texts

            # 从整个对象中提取文字
            texts = extract_text_recursive(res_dict)
            full_text_list.extend(texts)

        full_text = " ".join(full_text_list)
        print(f"📄 提取文本(前100字): {full_text[:100]}...")

        # 提取关键信息
        ie_result = ie(full_text)
        print(f"🧠 AI理解结果: {ie_result}")

        return {
            "code": 200, 
            "data": {
                "entities": ie_result[0] if ie_result else {}, 
                "tables": report_tables
            }
        }

    except Exception as e:
        import traceback
        traceback.print_exc()
        return {"code": 500, "msg": f"服务端内部错误: {str(e)}"}

if __name__ == '__main__':
    uvicorn.run(app, host="0.0.0.0", port=8000)