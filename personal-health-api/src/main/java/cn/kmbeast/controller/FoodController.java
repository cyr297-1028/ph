package cn.kmbeast.controller;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.mapper.UserDietRecordMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.entity.UserDietRecord;
import cn.kmbeast.utils.PathUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/food")
public class FoodController {

    @Resource
    private UserDietRecordMapper userDietRecordMapper;

    private static final String PY_MEAL_AI_URL = "http://127.0.0.1:60061/vision/meal_recognition";

    @PostMapping("/identifyMeal")
    public Result<Object> identifyMeal(@RequestParam("files") MultipartFile[] files) {
        try {
            Integer userId = LocalThreadHolder.getUserId();
            if (userId == null) return ApiResult.error("请先登录");

            // 1. 组装多文件 OkHttp 请求
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(120, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS) // 多图处理时间较长
                    .build();

            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            List<File> tempFiles = new ArrayList<>();

            for (MultipartFile file : files) {
                String savePath = PathUtils.getClassLoadRootPath() + "/pic/food/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
                File saveFile = new File(savePath);
                if (!saveFile.getParentFile().exists()) saveFile.getParentFile().mkdirs();
                file.transferTo(saveFile);
                tempFiles.add(saveFile);

                okhttp3.RequestBody fileBody = okhttp3.RequestBody.create(saveFile, MediaType.parse("image/jpeg"));
                builder.addFormDataPart("files", file.getOriginalFilename(), fileBody); // 注意：参数名是 "files"
            }

            Request request = new Request.Builder().url(PY_MEAL_AI_URL).post(builder.build()).build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JSONObject jsonObject = JSON.parseObject(response.body().string());

                    if (jsonObject.getInteger("code") == 200) {
                        JSONArray dataArray = jsonObject.getJSONArray("data");
                        List<UserDietRecord> savedRecords = new ArrayList<>();
                        LocalDateTime mealTime = LocalDateTime.now(); // 同一餐使用相同的时间戳

                        // 2. 遍历 Python 返回的每一道菜的结果，分别存入流水账表
                        for (int i = 0; i < dataArray.size(); i++) {
                            JSONObject item = dataArray.getJSONObject(i);
                            String foodName = item.getString("food_name");
                            JSONObject details = item.getJSONObject("details");

                            UserDietRecord record = new UserDietRecord();
                            record.setUserId(userId);
                            record.setFoodName(foodName);
                            record.setCreateTime(mealTime);

                            if (details != null && details.getString("food_description") != null) {
                                String desc = details.getString("food_description");
                                record.setCalories(extractValue(desc, "Calories:\\s*(\\d+\\.?\\d*)"));
                                record.setProtein(extractValue(desc, "Protein:\\s*(\\d+\\.?\\d*)"));
                                record.setFat(extractValue(desc, "Fat:\\s*(\\d+\\.?\\d*)"));
                                record.setCarbs(extractValue(desc, "Carbs:\\s*(\\d+\\.?\\d*)"));
                            } else {
                                record.setCalories("0"); record.setProtein("0"); record.setFat("0"); record.setCarbs("0");
                            }
                            userDietRecordMapper.insert(record);
                            savedRecords.add(record);
                        }

                        return ApiResult.success("一餐识别完成，数据已全部存入档案！",savedRecords);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("系统错误: " + e.getMessage());
        }
        return ApiResult.error("识别失败");
    }

    private String extractValue(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        return matcher.find() ? matcher.group(1) : "0";
    }

    /**
     * 手动录入饮食记录
     */
    @PostMapping("/manualAdd")
    public Result<Object> manualAdd(@RequestBody UserDietRecord record) {
        Integer userId = LocalThreadHolder.getUserId();
        if (userId == null) return ApiResult.error("请先登录");

        record.setUserId(userId);
        record.setCreateTime(LocalDateTime.now());

        // 保证空值有默认值
        if(record.getCalories() == null || record.getCalories().isEmpty()) record.setCalories("0");
        if(record.getProtein() == null || record.getProtein().isEmpty()) record.setProtein("0");
        if(record.getFat() == null || record.getFat().isEmpty()) record.setFat("0");
        if(record.getCarbs() == null || record.getCarbs().isEmpty()) record.setCarbs("0");

        userDietRecordMapper.insert(record);
        return ApiResult.success("手动打卡成功！");
    }

    /**
     * 获取今日打卡记录，用于前端表格和 ECharts 图表展示
     */
    @GetMapping("/todayList")
    public Result<List<UserDietRecord>> getTodayList() {
        Integer userId = LocalThreadHolder.getUserId();
        if (userId == null) return ApiResult.error("请先登录");

        List<UserDietRecord> records = userDietRecordMapper.getTodayRecords(userId);
        return ApiResult.success(records);
    }
}