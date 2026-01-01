import uvicorn
from fastapi import FastAPI, File, Form, UploadFile
# 导入 PPStructureV3 并别名
from paddleocr import PPStructureV3 as PPStructure
from paddlenlp import Taskflow
import os
import cv2
import numpy as np

app = FastAPI()

# 移除 show_log 和 image_orientation 参数，直接初始化
# V3 版本会自动加载默认模型
table_engine = PPStructure()

# 初始化信息抽取模型
schema = ['姓名', '性别', '年龄', '采样时间', '检测时间', '临床诊断']
ie = Taskflow('information_extraction', schema=schema)

@app.post("/ocr/medical_report")
async def analyze_medical_report(file: UploadFile = File(...)):
    if not os.path.exists(file_path):
        return {"code": 400, "msg": "文件不存在"}

    try:
        print(f"开始处理文件: {file_path}") # 手动打印日志替代 show_log
        
        # 读取图片
        img = cv2.imread(file_path)
        
        # 调用模型识别
        result = table_engine(img)
        
        report_tables = []
        full_text_list = []

        # 遍历识别结果
        for line in result:
            # 移除图片数据以减小包体
            if hasattr(line, 'pop'): 
                line.pop('img', None)
            elif isinstance(line, dict):
                line.pop('img', None)
            
            # 统一获取数据对象
            res_data = line
            
            # 处理表格
            # V3 返回的结构中，type 可能在 rec_type 或 type 字段，或者直接判断结构
            # 这里做防御性编程，尝试获取 HTML
            if isinstance(res_data, dict):
                if res_data.get('type') == 'table':
                    report_tables.append(res_data.get('res'))
                
                # 提取文本
                if 'res' in res_data:
                    content = res_data['res']
                    if isinstance(content, list):
                        for text_region in content:
                            if isinstance(text_region, dict) and 'text' in text_region:
                                full_text_list.append(text_region['text'])
                            elif isinstance(text_region, str):
                                full_text_list.append(text_region)
                    elif isinstance(content, dict) and 'html' in content:
                        # 某些版本表格只返回 html，不返回分散文本，这里可能需要额外处理
                        pass 

        # 拼接全文用于实体提取
        full_text_content = " ".join(full_text_list)
        print(f"提取到的文本: {full_text_content[:100]}...") # 打印前100个字看效果

        # 提取关键信息
        ie_result = ie(full_text_content)

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
        return {"code": 500, "msg": str(e)}

if __name__ == '__main__':
    uvicorn.run(app, host="0.0.0.0", port=8000)