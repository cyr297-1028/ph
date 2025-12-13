package cn.kmbeast.controller;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.PageResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.UserHealthQueryDto;
import cn.kmbeast.pojo.vo.UserHealthVO;
import cn.kmbeast.service.UserHealthService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;
import okhttp3.RequestBody;
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
    private static final String API_KEY = "sk-GzQCWUJt16DmMCUYHW1W66VvFdxDooIC5GUNSu1OirpKrwOa";

    /**
     * 构建请求体
     */
    private static JSONObject createRequestBody(String question) {
        JSONObject requestBody = new JSONObject();
        JSONArray messages = new JSONArray();

        // System Prompt: 设定 Kimi 的角色为健康助手
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
        requestBody.put("model", "moonshot-v1-8k");
        requestBody.put("temperature", 0.3); // 降低温度以获得更严谨的分析
        return requestBody;
    }

    /**
     * 发送 HTTP 请求
     */
    private static String sendPostRequest(JSONObject jsonBody) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.MINUTES) // 分析可能需要较长时间，增加超时
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
            if (!response.isSuccessful()) throw new IOException("请求 Kimi 失败: " + response);
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
        // 如果需要限制条数，可以在 DTO 中设置分页参数
        // queryDto.setCurrent(1);
        // queryDto.setSize(50);

        Result<List<UserHealthVO>> queryResult = userHealthService.query(queryDto);
        List<UserHealthVO> healthDataList = null;

        // Result 类本身没有 getData()，需要判断实际类型并强转
        if (queryResult instanceof PageResult) {
            healthDataList = ((PageResult<List<UserHealthVO>>) queryResult).getData();
        } else if (queryResult instanceof ApiResult) {
            healthDataList = ((ApiResult<List<UserHealthVO>>) queryResult).getData();
        }

        if (healthDataList == null || healthDataList.isEmpty()) {
            return ApiResult.success("您暂时还没有录入足够的健康数据，无法进行分析。请先去【用户健康管理】记录您的身体指标吧！");
        }

        // 3. 格式化数据为 Prompt
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("这是我的近期健康监测记录，请帮我生成一份【个人健康档案】和【健康趋势分析报告】。重点关注数值的变化趋势和异常项：\n\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // 限制投喂的数据量，取最近的 30-50 条即可，防止 Prompt 过长
        int limit = Math.min(healthDataList.size(), 50);
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

        // 4. 调用 Kimi API
        try {
            JSONObject requestBody = createRequestBody(promptBuilder.toString());
            String response = sendPostRequest(requestBody);
            String analysisResult = parse(response);
            return ApiResult.success(analysisResult);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("智能分析服务暂时繁忙，请稍后再试");
        }
    }
}