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
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(value = "/kimi")
public class KimiController {

    @Resource
    private UserHealthService userHealthService;

    private static final MediaType mediaType = MediaType.get("application/json; charset=utf-8");
    private static final String API_URL = "https://api.moonshot.cn/v1/chat/completions";
    // 请替换为你的实际 API Key
    private static final String API_KEY = "xxxx";

    /**
     * 构建请求体
     */
    private static JSONObject createRequestBody(String question) {
        JSONObject requestBody = new JSONObject();
        JSONArray messages = new JSONArray();

        // System Prompt: 设定 Kimi 的角色为健康助手
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("content", "你是 Kimi，一个专业的个人健康助手。请根据用户提供的健康监测数据，从医学角度进行客观、科学的趋势分析，并生成一份简要的个人健康档案。请指出异常指标，识别潜在风险，并给出生活方式或就医建议。回答要条理清晰，语气亲切。Moonshot AI 为专有名词，不可翻译成其他语言。");
        systemMessage.put("role", "system");
        messages.add(systemMessage);

        // User Message
        JSONObject userMessage = new JSONObject();
        userMessage.put("content", question);
        userMessage.put("role", "user");
        messages.add(userMessage);

        requestBody.put("messages", messages);
        requestBody.put("model", "moonshot-v1-8k");
        requestBody.put("temperature", 0.3); // 降低温度以获得更严谨的分析
        return requestBody;
    }

    /**
     * 发送 HTTP 请求
     */
    private static String sendPostRequest(JSONObject jsonBody) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.MINUTES)
                .readTimeout(2, TimeUnit.MINUTES)
                .writeTimeout(2, TimeUnit.MINUTES)
                .build();
        String jsonString = jsonBody.toString();
        RequestBody body = RequestBody.create(jsonString, mediaType);
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            return response.body().string();
        }
    }

    /**
     * 解析响应
     */
    private static String parse(String jsonResponse){
        JSONObject jsonObject = JSON.parseObject(jsonResponse);
        JSONArray choices = jsonObject.getJSONArray("choices");
        JSONObject choice = choices.getJSONObject(0);
        JSONObject message = choice.getJSONObject("message");
        return message.getString("content");
    }

    /**
     * 通用的问答接口
     */
    @PostMapping(value = "/seek")
    public Result<String> ask(@RequestBody Map<String, String> request) throws IOException {
        JSONObject requestBody = createRequestBody(request.get("question"));
        String response = sendPostRequest(requestBody);
        String parseJson = parse(response);
        return ApiResult.success(parseJson);
    }

    /**
     * 个性化健康档案与趋势分析接口
     * 获取当前用户的健康数据，发送给 Kimi 进行分析
     */
    @GetMapping(value = "/analyze")
    public Result<String> analyzeHealth() throws IOException {
        // 1. 获取当前登录用户 ID
        Integer userId = LocalThreadHolder.getUserId();
        if (userId == null) {
            return ApiResult.error("用户未登录");
        }

        // 2. 查询该用户的所有健康记录
        UserHealthQueryDto queryDto = new UserHealthQueryDto();
        queryDto.setUserId(userId);
        // 这里可以根据需要限制查询数量，例如最近 50 条，避免 token 超限
        // queryDto.setPage(1);
        // queryDto.setSize(50);

        Result<List<UserHealthVO>> queryResult = userHealthService.query(queryDto);
        List<UserHealthVO> healthDataList = queryResult.getData();

        if (healthDataList == null || healthDataList.isEmpty()) {
            return ApiResult.success("您暂时还没有录入足够的健康数据，无法进行分析。请先去【用户健康管理】记录您的身体指标吧！");
        }

        // 3. 格式化数据为 Prompt
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请根据我以下的健康监测记录，生成一份【个人健康档案】和【健康趋势分析报告】：\n\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (UserHealthVO data : healthDataList) {
            promptBuilder.append(String.format("- 时间：%s | 项目：%s | 数值：%s %s (参考范围：%s)\n",
                    data.getCreateTime().format(formatter),
                    data.getName(),
                    data.getValue(),
                    data.getUnit(),
                    data.getValueRange()
            ));
        }

        promptBuilder.append("\n请重点分析数值变化趋势，指出是否存在异常风险，并给出饮食和运动建议。");

        // 4. 调用 Kimi API
        JSONObject requestBody = createRequestBody(promptBuilder.toString());
        String response = sendPostRequest(requestBody);
        String analysisResult = parse(response);

        // 5. 返回分析结果
        return ApiResult.success(analysisResult);
    }
}