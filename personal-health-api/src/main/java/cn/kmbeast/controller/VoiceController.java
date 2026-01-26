package cn.kmbeast.controller;

import cn.kmbeast.pojo.api.Result;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.asr.v20190614.AsrClient;
import com.tencentcloudapi.asr.v20190614.models.CreateRecTaskRequest; // 录音文件识别（长语音）
import com.tencentcloudapi.asr.v20190614.models.SentenceRecognitionRequest; // 一句话识别（短语音，推荐）
import com.tencentcloudapi.asr.v20190614.models.SentenceRecognitionResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Base64;

@RestController
@RequestMapping("/voice")
public class VoiceController {

    // 如果自己用的话请替换为您的腾讯云密钥，这里用的是本人的
    private static final String SECRET_ID = "AKIDFF3e2zvkNq7C92jDWdXKuJVGtJWXL2c0";
    private static final String SECRET_KEY = "vhINK4cx87tSItHTSNC0s8E9tUjpjQ5Z";

    @PostMapping("/recognize")
    public Result<String> recognize(@RequestParam("file") MultipartFile file) {
        try {
            // 实例化认证对象
            Credential cred = new Credential(SECRET_ID, SECRET_KEY);
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("asr.tencentcloudapi.com");
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            AsrClient client = new AsrClient(cred, "ap-shanghai", clientProfile);

            // 构造请求（这里使用一句话识别，适合短语音指令）
            SentenceRecognitionRequest req = new SentenceRecognitionRequest();
            req.setProjectId(0L);
            req.setSubServiceType(2L); // 2：销售实时录音（也可以选其他引擎）
            req.setEngSerViceType("16k_zh"); // 16k 中文
            req.setSourceType(1L); // 1：语音数据
            req.setVoiceFormat("mp3"); // 音频格式，需与前端录音格式一致

            // 将文件转为 Base64
            String base64Audio = Base64.getEncoder().encodeToString(file.getBytes());
            req.setData(base64Audio);

            // 发送请求
            SentenceRecognitionResponse resp = client.SentenceRecognition(req);
            return Result.success(resp.getResult()); // 返回识别出的文本

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("语音识别失败：" + e.getMessage());
        }
    }
}