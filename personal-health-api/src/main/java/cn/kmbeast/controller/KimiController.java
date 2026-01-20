package cn.kmbeast.controller;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.pojo.api.ApiResult; // 虽然不用返回它了，但为了保持代码兼容可以留着
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.UserHealthQueryDto;
import cn.kmbeast.pojo.em.IsReadEnum;
import cn.kmbeast.pojo.em.MessageType;
import cn.kmbeast.pojo.entity.Message;
import cn.kmbeast.pojo.vo.UserHealthVO;
import cn.kmbeast.service.MessageService;
import cn.kmbeast.service.UserHealthService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(value = "/kimi")
public class KimiController {

    @Resource
    private UserHealthService userHealthService;

    @Resource
    private MessageService messageService;

    private static final MediaType mediaType = MediaType.get("application/json; charset=utf-8");
    private static final String API_URL = "https://api.moonshot.cn/v1/chat/completions";
    //API KEY
    private static final String API_KEY = "sk-erWCzbuAUBFdKQlD6jsln7HQjHRYWUJxLoWMKFXEDVOxcseE";

    /**
     * 构建请求体 (参数保持不变)
     */
    private static JSONObject createRequestBody(String question) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "kimi-k2-thinking");
        requestBody.put("temperature", 1.0);
        requestBody.put("max_tokens", 32768);
        requestBody.put("top_p", 1.0);
        requestBody.put("stream", false);

        JSONArray messages = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是 Kimi，一个专业的个人健康助手。请根据用户提供的健康监测数据，从医学角度进行客观、科学的趋势分析，并生成一份简要的个人健康档案。请指出异常指标，识别潜在风险，并给出生活方式或就医建议。");
        messages.add(systemMessage);

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", question);
        messages.add(userMessage);

        requestBody.put("messages", messages);
        return requestBody;
    }

    private static String sendPostRequest(JSONObject jsonBody) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .build();

        okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonBody.toString());
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API请求失败: " + response.code());
            }
            return response.body().string();
        }
    }

    private static String parse(String jsonResponse){
        try {
            JSONObject jsonObject = JSON.parseObject(jsonResponse);
            if (jsonObject.containsKey("error")) return "API Error: " + jsonObject.getString("error");
            JSONArray choices = jsonObject.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) return "未获取到有效回答";
            return choices.getJSONObject(0).getJSONObject("message").getString("content");
        } catch (Exception e) {
            return "解析响应异常";
        }
    }

    /**
     * 【核心修改】改为直接文件下载接口
     * 不再返回 JSON 对象，而是直接写入 HttpServletResponse 流
     */
    @GetMapping(value = "/analyze")
    public void analyzeHealth(HttpServletResponse response) throws IOException {
        // 设置响应头，告诉浏览器这是一个要下载的文件
        response.setContentType("text/plain; charset=UTF-8");
        String fileName = URLEncoder.encode("个人健康分析报告.txt", "UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        // 获取输出流
        PrintWriter writer = response.getWriter();

        Integer userId = LocalThreadHolder.getUserId();
        if (userId == null) {
            writer.write("错误：用户未登录，无法生成报告。");
            return;
        }

        // 查询数据
        UserHealthQueryDto queryDto = new UserHealthQueryDto();
        queryDto.setUserId(userId);
        Result<List<UserHealthVO>> queryResult = userHealthService.query(queryDto);
        List<UserHealthVO> healthDataList = queryResult.getData();

        if (healthDataList == null || healthDataList.isEmpty()) {
            writer.write("提示：您暂时还没有录入足够的健康数据，无法进行分析。\n请先去【用户健康管理】记录您的身体指标吧！");
            return;
        }

        // 构建提示词
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("这是我的近期健康监测记录，请分析：\n\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        int limit = Math.min(healthDataList.size(), 100);
        for (int i = 0; i < limit; i++) {
            UserHealthVO data = healthDataList.get(i);
            promptBuilder.append(String.format("- 时间：%s | 项目：%s | 数值：%s %s (正常范围：%s)\n",
                    data.getCreateTime().format(formatter), data.getName(), data.getValue(), data.getUnit(),
                    data.getValueRange() == null ? "无" : data.getValueRange()));
        }
        promptBuilder.append("\n请生成：1. 总体评分；2. 异常解读；3. 风险预警；4. 建议。");

        try {
            // 调用 AI
            String aiResponse = sendPostRequest(createRequestBody(promptBuilder.toString()));
            String analysisResult = parse(aiResponse);

            // 1. 将结果写入下载文件
            writer.write("====== AI 个人健康分析报告 ======\n\n");
            writer.write("生成时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n\n");
            writer.write(analysisResult);

            // 2. 尝试保存到数据库（即使失败也不影响文件下载）
            try {
                if (analysisResult.length() > 50) {
                    Message message = new Message();
                    message.setReceiverId(userId);
                    message.setContent("【健康报告已生成】\n" + analysisResult); // 注意：数据库字段必须是 LONGTEXT
                    message.setMessageType(MessageType.SYSTEM_INFO.getType());
                    message.setCreateTime(LocalDateTime.now());
                    message.setIsRead(IsReadEnum.READ_NO.getStatus());
                    messageService.save(Collections.singletonList(message));
                }
            } catch (Exception dbEx) {
                // 仅在后端控制台记录数据库错误，不影响用户下载文件
                System.err.println("报告保存数据库失败（可能是字段长度问题）：" + dbEx.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            writer.write("生成报告时发生错误：" + e.getMessage());
        } finally {
            writer.flush();
            writer.close();
        }
    }
}