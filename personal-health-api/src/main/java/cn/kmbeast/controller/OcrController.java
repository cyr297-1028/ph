package cn.kmbeast.controller;

import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.utils.PathUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/ocr")
public class OcrController {

    @Resource
    private FileController fileController; // 复用你现有的文件上传逻辑

    // Python服务的地址
    private static final String PY_OCR_URL = "http://127.0.0.1:8000/ocr/medical_report";

    @PostMapping("/recognition")
    public Result<Object> recognizeReport(@RequestParam("file") MultipartFile file) {
        try {
            // 1. 先保存图片到本地 (复用你FileController的逻辑或重写)
            // 假设我们生成一个文件名
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            // 这里为了演示，直接使用你FileController中定义的上传路径逻辑
            // 注意：Python服务需要能访问到这个物理路径，或者你需要把文件流传给Python
            // 简单起见，这里假设Java和Python运行在同一台机器，或者挂载了共享目录

            // 具体的保存路径
            String savePath = PathUtils.getClassLoadRootPath() + "/pic/" + fileName;
            File saveFile = new File(savePath);
            if (!saveFile.getParentFile().exists()) {
                saveFile.getParentFile().mkdirs();
            }
            file.transferTo(saveFile);

            // 2. 调用 Python OCR 服务
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            // 发送文件绝对路径给Python (表单字段 file_path)
            okhttp3.RequestBody formBody = new FormBody.Builder()
                    .add("file_path", savePath)
                    .build();

            Request request = new Request.Builder()
                    .url(PY_OCR_URL)
                    .post(formBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String resultJson = response.body().string();
                    JSONObject jsonObject = JSON.parseObject(resultJson);

                    if (jsonObject.getInteger("code") == 200) {
                        return ApiResult.success(jsonObject.get("data"));
                    } else {
                        return ApiResult.error("识别失败: " + jsonObject.getString("msg"));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ApiResult.error("服务调用异常: " + e.getMessage());
        }
        return ApiResult.error("未知错误");
    }
}