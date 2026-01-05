package cn.kmbeast.controller;

import cn.kmbeast.aop.Pager;
import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.base.QueryDto;
import cn.kmbeast.pojo.dto.query.extend.UserHealthQueryDto;
import cn.kmbeast.pojo.entity.UserHealth;
import cn.kmbeast.pojo.vo.ChartVO;
import cn.kmbeast.pojo.vo.UserHealthVO;
import cn.kmbeast.service.UserHealthService;
import cn.kmbeast.utils.DateUtil;
import cn.kmbeast.utils.IdFactoryUtil;
import cn.kmbeast.utils.PathUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户健康记录的 Controller
 */
@RestController
@RequestMapping(value = "/user-health")
public class UserHealthController {

    @Resource
    private UserHealthService userHealthService;

    // 修改点：这里的地址必须和 Python 代码中的 @app.post 路径一致
    private static final String PYTHON_OCR_URL = "http://127.0.0.1:8000/ocr/medical_report";

    /**
     * 智能识别接口 (OCR)
     * 支持 jpg, png, pdf
     */
    @PostMapping(value = "/recognition")
    public Result<Object> recognition(@RequestParam("file") MultipartFile file) {
        try {
            // 1. 保存文件到本地临时目录
            String originalFilename = file.getOriginalFilename();
            // 防止文件名为空
            if (originalFilename == null) {
                originalFilename = "temp.jpg";
            }
            String suffix = originalFilename.contains(".") ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String fileName = IdFactoryUtil.getFileId() + suffix;

            File fileDir = new File(PathUtils.getClassLoadRootPath() + "/temp_ocr");
            if (!fileDir.exists()) {
                fileDir.mkdirs();
            }
            File localFile = new File(fileDir.getAbsolutePath() + "/" + fileName);
            file.transferTo(localFile);

            // 2. 调用 Python OCR 服务
            // 使用 OkHttp 发送 Multipart 请求
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS) // OCR 识别可能耗时较长
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();

            // 构建请求体
            okhttp3.RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", localFile.getName(),
                            okhttp3.RequestBody.create(localFile, MediaType.parse("application/octet-stream")))
                    .build();

            Request request = new Request.Builder()
                    .url(PYTHON_OCR_URL)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String resultJson = response.body().string();
                    JSONObject jsonObject = JSON.parseObject(resultJson);

                    if (jsonObject.getInteger("code") == 200) {
                        // 返回识别到的结构化数据 (List)
                        return ApiResult.success(jsonObject.get("data"));
                    } else {
                        return ApiResult.error("识别失败: " + jsonObject.getString("msg"));
                    }
                } else {
                    return ApiResult.error("OCR 服务响应异常: " + response.code());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ApiResult.error("文件处理或服务调用异常");
        }
    }

    /**
     * 下载健康记录导入模板
     */
    @GetMapping(value = "/template")
    public void downloadTemplate(HttpServletResponse response) {
        userHealthService.exportTemplate(response);
    }

    /**
     * 一键导入健康记录 (Excel)
     */
    @PostMapping(value = "/import")
    public Result<Void> importData(@RequestParam("file") MultipartFile file) {
        return userHealthService.importData(file);
    }

    @PostMapping(value = "/save")
    public Result<Void> save(@RequestBody List<UserHealth> userHealths) {
        return userHealthService.save(userHealths);
    }

    @PostMapping(value = "/batchDelete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        return userHealthService.batchDelete(ids);
    }

    @PutMapping(value = "/update")
    public Result<Void> update(@RequestBody UserHealth userHealth) {
        return userHealthService.update(userHealth);
    }

    @Pager
    @PostMapping(value = "/query")
    public Result<List<UserHealthVO>> query(@RequestBody UserHealthQueryDto userHealthQueryDto) {
        return userHealthService.query(userHealthQueryDto);
    }

    @GetMapping(value = "/timeQuery/{id}/{day}")
    public Result<List<UserHealthVO>> timeQuery(@PathVariable Integer id,
                                                @PathVariable Integer day) {
        Integer userId = LocalThreadHolder.getUserId();
        QueryDto queryDto = DateUtil.startAndEndTime(day);
        UserHealthQueryDto userHealthQueryDto = new UserHealthQueryDto();
        userHealthQueryDto.setHealthModelConfigId(id);
        userHealthQueryDto.setUserId(userId);
        userHealthQueryDto.setStartTime(queryDto.getStartTime());
        userHealthQueryDto.setEndTime(queryDto.getEndTime());
        return userHealthService.query(userHealthQueryDto);
    }

    @GetMapping(value = "/daysQuery/{day}")
    @ResponseBody
    public Result<List<ChartVO>> query(@PathVariable Integer day) {
        return userHealthService.daysQuery(day);
    }
}