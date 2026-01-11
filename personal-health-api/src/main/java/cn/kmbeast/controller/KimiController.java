package cn.kmbeast.controller;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.UserHealthQueryDto;
import cn.kmbeast.pojo.vo.UserHealthVO;
import cn.kmbeast.service.UserHealthService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;
// 删除 import okhttp3.RequestBody; 避免混淆，下面直接用全限定名
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(value = "/kimi")
public class KimiController {

    @Resource
    private UserHealthService userHealthService;

    private static final MediaType mediaType = MediaType.get("application/json; charset=utf-8");
    private static final String API_URL = "https://api.moonshot.cn/v1/chat/completions";
    private static final String API_KEY = "sk-erWCzbuAUBFdKQlD6jsln7HQjHRYWUJxLoWMKFXEDVOxcseE";

    /**
     * 构建请求体
     */
    private static JSONObject createRequestBody(String question) {
        JSONObject requestBody = new JSONObject();
        JSONArray messages = new JSONArray();

        // System Prompt
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("content", "你是 Kimi，一个专业的个人健康助手。请根据用户提供的健康监测数据，从医学角度进行客观、科学的趋势分析，并生成一份简要的个人健康档案。请指出异常指标，识别潜在风险，并给出生活方式或就医建议。回答要条理清晰，语气亲切。可以使用 Markdown 格式渲染。");
        systemMessage.put("role", "system");
        messages.add(systemMessage);

        // User Message
        JSONObject userMessage = new JSONObject();
        userMessage.put("content", question);
        userMessage.put("role", "user");
        messages.add(userMessage);

        requestBody.put("messages", messages);
        // 使用支持长上下文的模型
        requestBody.put("model", "moonshot-v1-128k");
        requestBody.put("temperature", 0.3);
        return requestBody;
    }

    /**
     * 发送 HTTP 请求
     */
    private static String sendPostRequest(JSONObject jsonBody) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.MINUTES) // 延长超时
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .build();
        String jsonString = jsonBody.toString();

        // 使用 okhttp3.RequestBody 避免冲突
        // OkHttp 3.x 参数顺序是 (MediaType, String)，4.x 扩展了 (String, MediaType)
        // 这里使用 (MediaType, String) 兼容性更好
        okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonString);

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                // 打印错误详情，方便调试
                throw new IOException("请求 Kimi 失败: " + response.code() + " " + (response.body() != null ? response.body().string() : ""));
            }
            return response.body().string();
        }
    }

    /**
     * 解析响应
     */
    private static String parse(String jsonResponse){
        JSONObject jsonObject = JSON.parseObject(jsonResponse);
        if (jsonObject.containsKey("error")) {
            return "API 调用出错: " + jsonObject.getJSONObject("error").getString("message");
        }
        JSONArray choices = jsonObject.getJSONArray("choices");
        JSONObject choice = choices.getJSONObject(0);
        JSONObject message = choice.getJSONObject("message");
        return message.getString("content");
    }

    @GetMapping(value = "/analyze")
    public Result<String> analyzeHealth() throws IOException {
        Integer userId = LocalThreadHolder.getUserId();
        if (userId == null) {
            return ApiResult.error("用户未登录");
        }

        UserHealthQueryDto queryDto = new UserHealthQueryDto();
        queryDto.setUserId(userId);

        // 直接调用 getData()
        Result<List<UserHealthVO>> queryResult = userHealthService.query(queryDto);
        List<UserHealthVO> healthDataList = queryResult.getData();

        if (healthDataList == null || healthDataList.isEmpty()) {
            return ApiResult.success("您暂时还没有录入足够的健康数据，无法进行分析。请先去【用户健康管理】记录您的身体指标吧！");
        }

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("这是我的近期健康监测记录，请帮我生成一份【个人健康档案】和【健康趋势分析报告】。重点关注数值的变化趋势和异常项：\n\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        int limit = Math.min(healthDataList.size(), 100);
        for (int i = 0; i < limit; i++) {
            UserHealthVO data = healthDataList.get(i);
            promptBuilder.append(String.format("- 时间：%s | 项目：%s | 数值：%s %s (正常范围：%s)\n",
                    data.getCreateTime().format(formatter),
                    data.getName(),
                    data.getValue(),
                    data.getUnit(),
                    data.getValueRange() == null ? "无" : data.getValueRange()
            ));
        }

        promptBuilder.append("\n请分点回答，包括：1. 总体健康评分（满分100估算）；2. 异常指标解读；3. 趋势风险预警；4. 针对性的饮食运动建议。");

        try {
            JSONObject requestBody = createRequestBody(promptBuilder.toString());
            String response = sendPostRequest(requestBody);
            String analysisResult = parse(response);

            // 在控制台打印结果，知道后端算出来没有
            System.out.println("================ Kimi 分析结果 ================");
            System.out.println(analysisResult);
            System.out.println("==============================================");

            return ApiResult.success(analysisResult);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("智能分析服务暂时繁忙: " + e.getMessage());
        }
    }
}