import uvicorn
from fastapi import FastAPI

# 从我们刚刚拆分的文件中导入路由
from medical_ocr import router as medical_router
from food_ocr import router as food_router

# 初始化主程序
app = FastAPI(title="个人健康系统 AI 整合服务")

# 挂载路由 (将两个文件里的接口合并到这一个应用里)
app.include_router(medical_router)
app.include_router(food_router)

@app.get("/")
def read_root():
    return {"status": "AI Service is running", "ports_active": [60061]}

if __name__ == '__main__':
    # 启动总服务
    print("🚀 正在启动 AI 整合服务，监听端口 60061...")
    uvicorn.run(app, host="0.0.0.0", port=60061)