package cn.kmbeast.controller;

import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.utils.PathUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*; // 引入 okhttp3
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/ocr")
public class OcrController {

    @Resource
    private FileController fileController;

    // 确保这个地址和 Python 控制台显示的端口一致
    private static final String PY_OCR_URL = "http://127.0.0.1:60061/ocr/medical_report";

    @PostMapping("/recognition")
    public Result<Object> recognizeReport(@RequestParam("file") MultipartFile file) {
        try {
            // 1. 保存图片到本地
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String savePath = PathUtils.getClassLoadRootPath() + "/pic/" + fileName;
            File saveFile = new File(savePath);
            if (!saveFile.getParentFile().exists()) {
                saveFile.getParentFile().mkdirs();
            }
            file.transferTo(saveFile);

            System.out.println("Java已保存图片路径: " + savePath);

            // 2. 准备调用 Python 服务
            // 大模型推理很慢，超时时间设置长一点 (10分钟)
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(300, TimeUnit.SECONDS)
                    .readTimeout(600, TimeUnit.SECONDS)
                    .writeTimeout(300, TimeUnit.SECONDS)
                    .build();

            // 【关键修正】：使用 okhttp3.MediaType
            okhttp3.MediaType mediaType = okhttp3.MediaType.parse("image/jpeg");

            // 【关键修正】：OkHttp 4.x 标准写法是 create(File, MediaType)
            // 之前的 create(MediaType, File) 在 4.x 中已过时或已被移除
            okhttp3.RequestBody fileBody = okhttp3.RequestBody.create(saveFile, mediaType);

            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getOriginalFilename(), fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(PY_OCR_URL)
                    .post(requestBody)
                    .build();

            System.out.println("正在请求 Python 服务: " + PY_OCR_URL);

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String resultJson = response.body().string();
                    System.out.println("Python 返回结果: " + resultJson);

                    JSONObject jsonObject = JSON.parseObject(resultJson);
                    if (jsonObject.getInteger("code") == 200) {
                        return ApiResult.success(jsonObject.get("data"));
                    } else {
                        return ApiResult.error("识别失败: " + jsonObject.getString("msg"));
                    }
                } else {
                    return ApiResult.error("OCR服务响应异常: HTTP " + response.code());
                }
            }

        } catch (ConnectException e) {
            e.printStackTrace();
            return ApiResult.error("连接 Python 服务失败，请确认 Python 脚本已运行且显示 'Uvicorn running'");
        } catch (java.net.SocketTimeoutException e) {
            e.printStackTrace();
            return ApiResult.error("模型推理超时，请耐心等待或检查后台日志");
        } catch (IOException e) {
            e.printStackTrace();
            return ApiResult.error("文件处理或网络异常: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("系统未知错误: " + e.getMessage());
        }
    }
}