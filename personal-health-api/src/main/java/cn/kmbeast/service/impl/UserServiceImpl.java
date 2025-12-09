package cn.kmbeast.service.impl;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.mapper.UserMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.PageResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.base.QueryDto;
import cn.kmbeast.pojo.dto.query.extend.UserQueryDto;
import cn.kmbeast.pojo.dto.update.UserLoginDTO;
import cn.kmbeast.pojo.dto.update.UserRegisterDTO;
import cn.kmbeast.pojo.dto.update.UserUpdateDTO;
import cn.kmbeast.pojo.em.LoginStatusEnum;
import cn.kmbeast.pojo.em.RoleEnum;
import cn.kmbeast.pojo.em.WordStatusEnum;
import cn.kmbeast.pojo.entity.User;
import cn.kmbeast.pojo.vo.ChartVO;
import cn.kmbeast.pojo.vo.UserVO;
import cn.kmbeast.service.UserService;
import cn.kmbeast.utils.DateUtil;
import cn.kmbeast.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.iai.v20200303.IaiClient;
import com.tencentcloudapi.iai.v20200303.models.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户服务实现类
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    // 读取配置文件中的腾讯云配置
    @Value("${tencentcloudapi.secretId}")
    private String secretId;
    @Value("${tencentcloudapi.secretKey}")
    private String secretKey;
    @Value("${tencentcloudapi.endpoint}")
    private String endpoint;
    @Value("${tencentcloudapi.region}")
    private String region;

    // 腾讯云人员库ID，需确保与控制台一致
    private static final String GROUP_ID = "yuedaoying";

    // 获取腾讯云客户端实例
    private IaiClient getClient() {
        Credential cred = new Credential(secretId, secretKey);
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint(endpoint);
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        return new IaiClient(cred, region, clientProfile);
    }

    /**
     * 人脸数据录入
     */
    @Override
    public Result<String> addFace(MultipartFile file, String userAccount) {
        try {
            String base64Img = java.util.Base64.getEncoder().encodeToString(file.getBytes());
            IaiClient client = getClient();

            // 1. 尝试获取人员库，不存在则创建
            try {
                GetGroupInfoRequest groupReq = new GetGroupInfoRequest();
                groupReq.setGroupId(GROUP_ID);
                client.GetGroupInfo(groupReq);
            } catch (Exception e) {
                try {
                    CreateGroupRequest createGroupReq = new CreateGroupRequest();
                    createGroupReq.setGroupName("健康管理人员库");
                    createGroupReq.setGroupId(GROUP_ID);
                    client.CreateGroup(createGroupReq);
                    log.info("自动创建人员库成功: {}", GROUP_ID);
                } catch (Exception ex) {
                    log.error("创建人员库失败", ex);
                }
            }

            // 2. 尝试创建人员（携带人脸）
            CreatePersonRequest req = new CreatePersonRequest();
            req.setGroupId(GROUP_ID);
            req.setPersonId(userAccount);
            req.setPersonName(userAccount);
            req.setImage(base64Img);

            try {
                client.CreatePerson(req);
            } catch (Exception e) {
                // 3. 如果人员已存在 (PersonIdAlreadyExist)，则调用增加人脸接口
                if (e.getMessage().contains("PersonIdAlreadyExist") || e.getMessage().contains("InvalidParameterValue.PersonIdAlreadyExist")) {
                    CreateFaceRequest faceReq = new CreateFaceRequest();
                    faceReq.setPersonId(userAccount);
                    faceReq.setImages(new String[]{base64Img});
                    // 【修正点1】删除 setGroupId，增加人脸不需要指定库ID
                    // faceReq.setGroupId(GROUP_ID);

                    client.CreateFace(faceReq);
                    log.info("用户 {} 已存在，追加人脸成功", userAccount);
                } else {
                    throw e;
                }
            }
            return ApiResult.success("人脸录入成功");

        } catch (Exception e) {
            log.error("人脸录入异常", e);
            return ApiResult.error("人脸录入失败：" + e.getMessage());
        }
    }

    /**
     * 人脸登录
     */
    @Override
    public Result<Object> faceLogin(MultipartFile file) {
        try {
            String base64Img = java.util.Base64.getEncoder().encodeToString(file.getBytes());
            IaiClient client = getClient();

            // 1. 在人员库中搜索人脸
            SearchFacesRequest req = new SearchFacesRequest();
            req.setGroupIds(new String[]{GROUP_ID});
            req.setImage(base64Img);
            req.setMaxPersonNum(1L);
            // 【修正点2】SearchFacesRequest 没有 setNeedFaceAttributes 方法，已删除
            // req.setNeedFaceAttributes(0L);
            req.setQualityControl(0L);

            SearchFacesResponse resp = client.SearchFaces(req);

            // 2. 解析结果
            if (resp.getResults() != null && resp.getResults().length > 0) {
                // 【修正点3】使用 Candidate 类替代 ResultCandidate
                Candidate[] candidates = resp.getResults()[0].getCandidates();

                if (candidates != null && candidates.length > 0) {
                    Candidate bestMatch = candidates[0];
                    // 【修正点4】现在可以正常调用 getScore() 和 getPersonId() 了
                    Float score = bestMatch.getScore();
                    String personId = bestMatch.getPersonId();

                    log.info("人脸识别结果 - 账号: {}, 分数: {}", personId, score);

                    if (score > 80) {
                        User user = userMapper.getByActive(User.builder().userAccount(personId).build());
                        if (user == null) {
                            return ApiResult.error("识别通过，但系统内未找到关联账号: " + personId);
                        }
                        if (user.getIsLogin()) {
                            return ApiResult.error("账号被禁用");
                        }

                        String token = JwtUtil.toToken(user.getId(), user.getUserRole());
                        Map<String, Object> map = new HashMap<>();
                        map.put("token", token);
                        map.put("role", user.getUserRole());
                        return ApiResult.success("刷脸登录成功", map);
                    } else {
                        return ApiResult.error("人脸匹配度不足(" + score.intValue() + ")，请重试");
                    }
                }
            }
            return ApiResult.error("未检测到匹配的人脸信息");

        } catch (Exception e) {
            log.error("人脸登录异常", e);
            String msg = "人脸识别服务异常";
            if(e.getMessage() != null && e.getMessage().contains("ResourceUnavailable.InArrears")) {
                msg = "服务欠费不可用";
            } else if (e.getMessage() != null && e.getMessage().contains("InvalidParameterValue.NoFaceInPhoto")) {
                msg = "未检测到人脸，请正对摄像头";
            }
            return ApiResult.error(msg);
        }
    }

    // ---------------- 原有业务逻辑保持不变 ----------------

    @Override
    public Result<String> register(UserRegisterDTO userRegisterDTO) {
        User user = userMapper.getByActive(
                User.builder().userName(userRegisterDTO.getUserName()).build()
        );
        if (Objects.nonNull(user)) {
            return ApiResult.error("用户名已经被使用，请换一个");
        }
        User entity = userMapper.getByActive(
                User.builder().userAccount(userRegisterDTO.getUserAccount()).build()
        );
        if (Objects.nonNull(entity)) {
            return ApiResult.error("账号不可用");
        }
        User saveEntity = User.builder()
                .userRole(RoleEnum.USER.getRole())
                .userName(userRegisterDTO.getUserName())
                .userAccount(userRegisterDTO.getUserAccount())
                .userAvatar(userRegisterDTO.getUserAvatar())
                .userPwd(userRegisterDTO.getUserPwd())
                .userEmail(userRegisterDTO.getUserEmail())
                .createTime(LocalDateTime.now())
                .isLogin(LoginStatusEnum.USE.getFlag())
                .isWord(WordStatusEnum.USE.getFlag()).build();
        userMapper.insert(saveEntity);
        return ApiResult.success("注册成功");
    }

    @Override
    public Result<Object> login(UserLoginDTO userLoginDTO) {
        User user = userMapper.getByActive(
                User.builder().userAccount(userLoginDTO.getUserAccount()).build()
        );
        if (!Objects.nonNull(user)) {
            return ApiResult.error("账号不存在");
        }
        if (!Objects.equals(userLoginDTO.getUserPwd(), user.getUserPwd())) {
            return ApiResult.error("密码错误");
        }
        if (user.getIsLogin()) {
            return ApiResult.error("登录状态异常");
        }
        String token = JwtUtil.toToken(user.getId(), user.getUserRole());
        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        map.put("role", user.getUserRole());
        return ApiResult.success("登录成功", map);
    }

    @Override
    public Result<UserVO> auth() {
        Integer userId = LocalThreadHolder.getUserId();
        User queryEntity = User.builder().id(userId).build();
        User user = userMapper.getByActive(queryEntity);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return ApiResult.success(userVO);
    }

    @Override
    public Result<List<User>> query(UserQueryDto userQueryDto) {
        List<User> users = userMapper.query(userQueryDto);
        Integer count = userMapper.queryCount(userQueryDto);
        return PageResult.success(users, count);
    }

    @Override
    public Result<String> update(UserUpdateDTO userUpdateDTO) {
        User updateEntity = User.builder().id(LocalThreadHolder.getUserId()).build();
        BeanUtils.copyProperties(userUpdateDTO, updateEntity);
        userMapper.update(updateEntity);
        return ApiResult.success();
    }

    @Override
    public Result<String> batchDelete(List<Integer> ids) {
        userMapper.batchDelete(ids);
        return ApiResult.success();
    }

    @Override
    public Result<String> updatePwd(Map<String, String> map) {
        String oldPwd = map.get("oldPwd");
        String newPwd = map.get("newPwd");
        User user = userMapper.getByActive(
                User.builder().id(LocalThreadHolder.getUserId()).build()
        );
        if (!user.getUserPwd().equals(oldPwd)) {
            return ApiResult.error("原始密码验证失败");
        }
        user.setUserPwd(newPwd);
        userMapper.update(user);
        return ApiResult.success();
    }

    @Override
    public Result<UserVO> getById(Integer id) {
        User user = userMapper.getByActive(User.builder().id(id).build());
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return ApiResult.success(userVO);
    }

    @Override
    public Result<String> insert(UserRegisterDTO userRegisterDTO) {
        return register(userRegisterDTO);
    }

    @Override
    public Result<String> backUpdate(User user) {
        userMapper.update(user);
        return ApiResult.success();
    }

    @Override
    public Result<List<ChartVO>> daysQuery(Integer day) {
        QueryDto queryDto = DateUtil.startAndEndTime(day);
        UserQueryDto userQueryDto = new UserQueryDto();
        userQueryDto.setStartTime(queryDto.getStartTime());
        userQueryDto.setEndTime(queryDto.getEndTime());
        List<User> userList = userMapper.query(userQueryDto);
        List<LocalDateTime> localDateTimes = userList.stream().map(User::getCreateTime).collect(Collectors.toList());
        List<ChartVO> chartVOS = DateUtil.countDatesWithinRange(day, localDateTimes);
        return ApiResult.success(chartVOS);
    }
}