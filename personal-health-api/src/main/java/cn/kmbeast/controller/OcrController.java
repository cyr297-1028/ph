package cn.kmbeast.controller;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.mapper.HealthModelConfigMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.HealthModelConfigQueryDto;
import cn.kmbeast.pojo.entity.HealthModelConfig;
import cn.kmbeast.pojo.entity.UserHealth;
import cn.kmbeast.pojo.vo.HealthModelConfigVO;
import cn.kmbeast.service.UserHealthService;
import cn.kmbeast.utils.PathUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/ocr")
public class OcrController {

    @Resource
    private UserHealthService userHealthService;
    @Resource
    private HealthModelConfigMapper healthModelConfigMapper;

    private static final String PY_OCR_URL = "http://127.0.0.1:60061/ocr/medical_report";

    @PostMapping("/recognition")
    public Result<Object> recognizeReport(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String savePath = PathUtils.getClassLoadRootPath() + "/pic/" + fileName;
            File saveFile = new File(savePath);
            if (!saveFile.getParentFile().exists()) {
                saveFile.getParentFile().mkdirs();
            }
            file.transferTo(saveFile);

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(300, TimeUnit.SECONDS)
                    .readTimeout(600, TimeUnit.SECONDS)
                    .writeTimeout(300, TimeUnit.SECONDS)
                    .build();

            okhttp3.MediaType mediaType = okhttp3.MediaType.parse("image/jpeg");
            okhttp3.RequestBody fileBody = okhttp3.RequestBody.create(saveFile, mediaType);

            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getOriginalFilename(), fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(PY_OCR_URL)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String resultJson = response.body().string();
                    JSONObject jsonObject = JSON.parseObject(resultJson);

                    if (jsonObject.getInteger("code") == 200) {
                        String ocrText = jsonObject.getString("data");

                        int savedCount = parseAndSaveData(ocrText);

                        if (savedCount > 0) {
                            String msg = "识别成功！已自动匹配并保存 " + savedCount + " 项数据。\n请点击“确定”查看结果。";
                            // 【修复关键】将消息也作为 data 返回，防止前端因 data 为 null 而报错
                            return ApiResult.success(msg, msg);
                        } else {
                            String preview = ocrText.length() > 100 ? ocrText.substring(0, 100) + "..." : ocrText;
                            String msg = "识别完成，但未匹配到有效数据库项目。\n请检查后台【健康模型配置】。\n\nOCR预览:\n" + preview;
                            return ApiResult.success(msg, msg);
                        }
                    } else {
                        return ApiResult.error("识别失败: " + jsonObject.getString("msg"));
                    }
                } else {
                    return ApiResult.error("OCR服务响应异常: HTTP " + response.code());
                }
            }

        } catch (ConnectException e) {
            return ApiResult.error("连接 Python 服务失败，请确认脚本已运行");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("系统错误: " + e.getMessage());
        }
    }

    private int parseAndSaveData(String text) {
        if (text == null || text.isEmpty()) return 0;

        List<HealthModelConfigVO> configs = healthModelConfigMapper.query(new HealthModelConfigQueryDto());
        if (CollectionUtils.isEmpty(configs)) return 0;

        List<UserHealth> saveList = new ArrayList<>();
        Integer userId = LocalThreadHolder.getUserId();
        if (userId == null) return 0;

        String[] lines = text.split("\n");
        Pattern numberPattern = Pattern.compile("(\\d+\\.?\\d*)");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            for (HealthModelConfigVO config : configs) {
                String dbName = config.getName();

                if (dbName == null || dbName.trim().length() < 1) {
                    continue;
                }

                if (line.contains(dbName)) {
                    int index = line.indexOf(dbName);
                    String suffix = line.substring(index + dbName.length());
                    Matcher matcher = numberPattern.matcher(suffix);

                    if (matcher.find()) {
                        String valueStr = matcher.group(1);
                        UserHealth userHealth = new UserHealth();
                        userHealth.setUserId(userId);
                        userHealth.setHealthModelConfigId(config.getId());
                        userHealth.setValue(valueStr);
                        userHealth.setCreateTime(LocalDateTime.now());
                        saveList.add(userHealth);
                        System.out.println(">>> [精准匹配] OCR: " + dbName + " -> 值: " + valueStr);
                        break;
                    }
                }
            }
        }

        if (!saveList.isEmpty()) {
            userHealthService.save(saveList);
        }
        return saveList.size();
    }
}