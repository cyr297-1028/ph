from fastapi import APIRouter, File, UploadFile
import requests
import os
import shutil
from requests_oauthlib import OAuth2Session
from oauthlib.oauth2 import BackendApplicationClient
from transformers import pipeline # 引入 HuggingFace 极简管道

router = APIRouter()

# --- FatSecret 配置 ---
CLIENT_ID = '2af3b0fb44564eeb875585b924024541'
CLIENT_SECRET = '0fcec8fff60a4bad89b4a3f141625fe9'
TOKEN_URL = 'https://oauth.fatsecret.com/connect/token'

def get_fatsecret_token():
    client = BackendApplicationClient(client_id=CLIENT_ID)
    oauth = OAuth2Session(client=client)
    token = oauth.fetch_token(token_url=TOKEN_URL, client_id=CLIENT_ID, client_secret=CLIENT_SECRET)
    return token['access_token']

# --- 加载 AI 食物识别大模型 ---
print("⏳ 正在加载 Food-101 图像识别模型 (首次运行会自动下载，请耐心等待~300MB)...")
try:
    # 这里使用的是开源社区极为成熟的 nateraw/food 模型，能识别 101 种常见食物
    food_classifier = pipeline("image-classification", model="nateraw/food")
    print("✅ 食物识别大模型加载成功！")
except Exception as e:
    print(f"❌ 模型加载失败: {e}")


@router.post("/ocr/food_recognition")
async def food_recognition(file: UploadFile = File(...)):
    temp_file = f"./temp_food_{file.filename}"
    try:
        # 1. 保存前端传来的图片
        with open(temp_file, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

        # 2. 【核心】让 AI 模型看图并预测食物名字！
        # classifier 会返回类似: [{'label': 'pizza', 'score': 0.99}, ...]
        predictions = food_classifier(temp_file)
        
        # 提取置信度最高（最准）的那个食物名字
        detected_food_name = predictions[0]['label']
        confidence = predictions[0]['score']
        print(f"🧐 AI 识别结果: {detected_food_name}, 把握度: {confidence:.2f}")

        # 3. 拿到名字后，调用 FatSecret API 获取营养数据
        token = get_fatsecret_token()
        search_url = "https://platform.fatsecret.com/rest/server.api"
        params = {
            "method": "foods.search",
            "search_expression": detected_food_name, # 把 AI 认出的名字传给 FatSecret
            "format": "json"
        }
        headers = {"Authorization": f"Bearer {token}"}
        
        response = requests.get(search_url, params=params, headers=headers)
        fatsecret_data = response.json()
        
        # 提取第一条结果
        food_list = fatsecret_data.get('foods', {}).get('food', [])
        details = food_list[0] if food_list else "未找到营养数据"

        # 清理临时图片
        if os.path.exists(temp_file):
            os.remove(temp_file)

        return {
            "code": 200,
            "data": {
                "food_name": detected_food_name,  # 例如 "pizza" 或 "apple_pie"
                "confidence": f"{confidence * 100:.1f}%", # 返回给前端装杯用：AI确信度
                "details": details
            }
        }
    except Exception as e:
        if os.path.exists(temp_file):
            os.remove(temp_file)
        return {"code": 500, "msg": f"食物识别失败: {str(e)}"}