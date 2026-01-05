import uvicorn
from fastapi import FastAPI, File, UploadFile
from transformers import Qwen2_5_VLForConditionalGeneration, AutoProcessor
from qwen_vl_utils import process_vision_info
import torch
import os
import shutil
import json

app = FastAPI()

# ==========================================
# 1. 初始化 SMR-R1 模型 (替代 PaddleOCR)
# ==========================================
MODEL_PATH = "mrlijun/SMR-R1"  # HuggingFace 模型 ID，第一次运行会自动下载

print("⏳ 正在加载 SMR-R1 模型 (这需要较多显存)...")
try:
    # 加载模型 (自动适配显卡)
    model = Qwen2_5_VLForConditionalGeneration.from_pretrained(
        MODEL_PATH,
        torch_dtype=torch.bfloat16,
        device_map="auto" 
    )
    # 加载处理器 (负责处理图片和文字)
    processor = AutoProcessor.from_pretrained(MODEL_PATH)
    print("✅ SMR-R1 模型加载成功！")
except Exception as e:
    print(f"❌ 模型加载失败 (请检查显存或CUDA配置): {e}")
    model = None
    processor = None

@app.post("/ocr/medical_report")
async def analyze_medical_report(file: UploadFile = File(...)):
    # 1. 保存图片到本地
    save_dir = "temp_uploads"
    if not os.path.exists(save_dir):
        os.makedirs(save_dir)
    file_path = os.path.join(save_dir, file.filename)
    
    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)
    print(f"✅ 图片已接收: {file_path}")

    if model is None:
        return {"code": 500, "msg": "模型未能成功启动，无法处理请求。"}

    try:
        # ==========================================
        # 2. 构造 Prompt (提示词)
        #    在这里告诉模型：你要分类，还要结构化提取
        # ==========================================
        prompt_text = """
        你是一个专业的医疗文档分析助手。请分析这张图片，完成以下任务：
        1. 【分类】：判断这张图片的类型（如：血常规检验报告、生化检验报告、尿液分析报告、处方单、其他）。
        2. 【提取】：提取表格中的所有检测项目。
        
        请严格按照以下 JSON 格式输出结果，不要包含 Markdown 格式：
        {
            "report_type": "报告类型",
            "items": [
                {"name": "项目名称", "result": "结果数值", "unit": "单位", "ref_range": "参考范围", "arrow": "异常箭头(↑/↓/无)"}
            ],
            "patient": {
                "name": "姓名",
                "sample_time": "采样时间"
            }
        }
        """

        # ==========================================
        # 3. 调用模型进行推理 (端到端)
        # ==========================================
        messages = [
            {
                "role": "user",
                "content": [
                    {"type": "image", "image": file_path},
                    {"type": "text", "text": prompt_text},
                ],
            }
        ]

        # 预处理输入
        text = processor.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
        image_inputs, video_inputs = process_vision_info(messages)
        inputs = processor(
            text=[text],
            images=image_inputs,
            padding=True,
            return_tensors="pt",
        )
        inputs = inputs.to("cuda") # 发送到显卡

        # 生成结果
        print("⏳ SMR-R1 正在思考和提取...")
        generated_ids = model.generate(**inputs, max_new_tokens=2048) # 允许生成的最大长度
        generated_ids_trimmed = [
            out_ids[len(in_ids) :] for in_ids, out_ids in zip(inputs.input_ids, generated_ids)
        ]
        output_text = processor.batch_decode(
            generated_ids_trimmed, skip_special_tokens=True, clean_up_tokenization_spaces=False
        )[0]

        print(f"🧠 模型原始输出: {output_text[:100]}...")

        # ==========================================
        # 4. 解析结果 (将模型的文本转回 JSON)
        # ==========================================
        try:
            # 有时候模型会输出 ```json ... ```，需要清洗一下
            clean_json_str = output_text.replace("```json", "").replace("```", "").strip()
            result_json = json.loads(clean_json_str)
            
            # 适配你前端需要的格式
            final_data = {
                "entities": result_json.get("patient", {}),  # 对应你原来的 entities
                "tables": [], # SMR-R1 直接提取了结构化 items，可能不需要原来的 html 表格了，或者你可以自己拼一个 html
                "structured_items": result_json.get("items", []), # 新增：结构化的项目列表
                "doc_type": result_json.get("report_type", "未知") # 新增：自动分类结果
            }
            
            return {"code": 200, "data": final_data}

        except json.JSONDecodeError:
            print("⚠️ 模型输出的不是标准 JSON，返回原始文本")
            return {"code": 200, "data": {"raw_text": output_text}}

    except Exception as e:
        import traceback
        traceback.print_exc()
        return {"code": 500, "msg": f"AI 推理失败: {str(e)}"}

if __name__ == '__main__':
    uvicorn.run(app, host="0.0.0.0", port=8000)