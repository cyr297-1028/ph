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
import sun.misc.BASE64Encoder; // 或者使用 java.util.Base64

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

    // 定义一个固定的人员库ID
    private static final String GROUP_ID = "PersonalHealthGroup";

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
     * 用户注册
     *
     * @param userRegisterDTO 注册入参
     * @return Result<String> 响应结果
     */
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

    /**
     * 用户登录
     *
     * @param userLoginDTO 登录入参
     * @return Result<String> 响应结果
     */
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

    /**
     * 令牌检验 -- 认证成功返回用户信息
     *
     * @return Result<UserVO>
     */
    @Override
    public Result<UserVO> auth() {
        Integer userId = LocalThreadHolder.getUserId();
        User queryEntity = User.builder().id(userId).build();
        User user = userMapper.getByActive(queryEntity);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return ApiResult.success(userVO);
    }

    /**
     * 分页查询用户数据
     *
     * @param userQueryDto 分页参数
     * @return Result<List < User>> 响应结果
     */
    @Override
    public Result<List<User>> query(UserQueryDto userQueryDto) {
        List<User> users = userMapper.query(userQueryDto);
        Integer count = userMapper.queryCount(userQueryDto);
        return PageResult.success(users, count);
    }

    /**
     * 用户信息修改
     *
     * @param userUpdateDTO 修改信息入参
     * @return Result<String> 响应结果
     */
    @Override
    public Result<String> update(UserUpdateDTO userUpdateDTO) {
        User updateEntity = User.builder().id(LocalThreadHolder.getUserId()).build();
        BeanUtils.copyProperties(userUpdateDTO, updateEntity);
        userMapper.update(updateEntity);
        return ApiResult.success();
    }


    /**
     * 批量删除用户信息
     */
    @Override
    public Result<String> batchDelete(List<Integer> ids) {
        userMapper.batchDelete(ids);
        return ApiResult.success();
    }

    /**
     * 用户信息修改密码
     *
     * @param map 修改信息入参
     * @return Result<String> 响应结果
     */
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

    /**
     * 通过ID查询用户信息
     *
     * @param id 用户ID
     */
    @Override
    public Result<UserVO> getById(Integer id) {
        User user = userMapper.getByActive(User.builder().id(id).build());
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return ApiResult.success(userVO);
    }

    /**
     * 后台新增用户
     *
     * @param userRegisterDTO 注册入参
     * @return Result<String> 响应结果
     */
    @Override
    public Result<String> insert(UserRegisterDTO userRegisterDTO) {
        return register(userRegisterDTO);
    }

    /**
     * 后台用户信息修改
     *
     * @param user 信息实体
     * @return Result<String> 响应结果
     */
    @Override
    public Result<String> backUpdate(User user) {
        userMapper.update(user);
        return ApiResult.success();
    }

    /**
     * 统计指定时间里面的用户存量数据
     *
     * @param day 天数
     * @return Result<List < ChartVO>>
     */
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
                } catch (Exception ex) {
                    log.error("创建人员库失败", ex);
                }
            }

            // 2. 尝试创建人员（携带人脸）
            CreatePersonRequest req = new CreatePersonRequest();
            req.setGroupId(GROUP_ID); // 创建人员时需要 GroupId
            req.setPersonId(userAccount);
            req.setPersonName(userAccount);
            req.setImage(base64Img);

            try {
                client.CreatePerson(req);
            } catch (Exception e) {
                // 3. 如果人员已存在 (PersonIdAlreadyExist)，则调用增加人脸接口
                if (e.getMessage().contains("InvalidParameterValue.PersonIdAlreadyExist")) {
                    CreateFaceRequest faceReq = new CreateFaceRequest();
                    // faceReq.setGroupId(GROUP_ID);
                    faceReq.setPersonId(userAccount); // 只需要指定 PersonId
                    faceReq.setImages(new String[]{base64Img});
                    client.CreateFace(faceReq);
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
            // 修正：删除 req.setMinScore(80F); 这一行，SDK请求对象里没这个方法

            SearchFacesResponse resp = client.SearchFaces(req);

            // 2. 解析结果
            if (resp.getResults() != null && resp.getResults().length > 0) {
                // 修正：将 ResultCandidate 改为 Candidate
                Candidate candidate = resp.getResults()[0].getCandidates()[0];

                // 现在 getScore() 和 getPersonId() 就能正常识别了
                Float score = candidate.getScore();

                if (score > 80) { // 二次校验分数
                    String userAccount = candidate.getPersonId();

                    // 3. 数据库查询用户
                    User user = userMapper.getByActive(User.builder().userAccount(userAccount).build());
                    if (user == null) {
                        return ApiResult.error("识别通过，但系统内未找到关联账号");
                    }
                    if (user.getIsLogin()) {
                        return ApiResult.error("账号被禁用");
                    }

                    // 4. 生成Token
                    String token = JwtUtil.toToken(user.getId(), user.getUserRole());
                    Map<String, Object> map = new HashMap<>();
                    map.put("token", token);
                    map.put("role", user.getUserRole());
                    return ApiResult.success("刷脸登录成功", map);
                }
            }
            return ApiResult.error("人脸识别未通过，请重试");

        } catch (Exception e) {
            log.error("人脸登录异常", e);
            return ApiResult.error("人脸识别服务异常");
        }
    }
}
