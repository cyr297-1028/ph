package cn.kmbeast.service.impl;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.mapper.EvaluationsMapper;
import cn.kmbeast.mapper.MessageMapper;
import cn.kmbeast.mapper.UserMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.PageResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.EvaluationsQueryDto;
import cn.kmbeast.pojo.dto.query.extend.MessageQueryDto;
import cn.kmbeast.pojo.em.MessageType;
import cn.kmbeast.pojo.entity.Evaluations;
import cn.kmbeast.pojo.entity.Message;
import cn.kmbeast.pojo.entity.User;
import cn.kmbeast.pojo.vo.CommentChildVO;
import cn.kmbeast.pojo.vo.CommentParentVO;
import cn.kmbeast.pojo.vo.EvaluationsVO;
import cn.kmbeast.service.EvaluationsService;
import cn.kmbeast.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 评论服务实现类
 */
@Service
public class EvaluationsServiceImpl implements EvaluationsService {

    @Resource
    private EvaluationsMapper evaluationsMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private MessageService messageService;
    @Autowired
    private MessageMapper messageMapper;

    /**
     * 评论
     *
     * @return Result<String>
     */
    @Override
    public Result<Object> insert(Evaluations evaluations) {
        evaluations.setCommenterId(LocalThreadHolder.getUserId());
        User queryConditionEntity = User.builder().id(LocalThreadHolder.getUserId()).build();
        User user = userMapper.getByActive(queryConditionEntity);
        if (user.getIsWord()) {
            return ApiResult.error("账户已被禁言");
        }
        evaluations.setCreateTime(LocalDateTime.now());
        evaluationsMapper.save(evaluations);
        EvaluationsQueryDto evaluationsQueryDto = new EvaluationsQueryDto();
        evaluationsQueryDto.setId(evaluations.getParentId());
        List<CommentChildVO> commentChildVOS = evaluationsMapper.query(evaluationsQueryDto);
        CommentChildVO commentChildVO = commentChildVOS.get(0);
        Integer userId = commentChildVO.getUserId();
        // 针对回复自我二级评论，不做处理
        if (Objects.equals(evaluations.getCommenterId(), evaluations.getReplierId())) {
            return ApiResult.success();
        }
        if (evaluations.getParentId() != null) {
            Message message = new Message();
            message.setContent(evaluations.getParentId() + ";" + evaluations.getContentId() + ";" + evaluations.getContent());
            message.setSenderId(LocalThreadHolder.getUserId());
            message.setReceiverId(evaluations.getReplierId() == null ? userId : evaluations.getReplierId());
            messageService.evaluationsReplySave(message);
        }
        return ApiResult.success("评论成功");
    }

    /**
     * 查询全部评论
     *
     * @return Result<String>
     */
    @Override
    public Result<Object> list(Integer contentId, String contentType) {
        List<CommentParentVO> parentComments = evaluationsMapper.getParentComments(contentId, contentType);
        setUpvoteFlag(parentComments);
        Integer count = evaluationsMapper.totalCount(contentId, contentType);
        return ApiResult.success(new EvaluationsVO(count, parentComments));
    }

    /**
     * 设置点赞状态
     *
     * @param parentComments 评论数据列表
     */
    private void setUpvoteFlag(List<CommentParentVO> parentComments) {
        String userId = LocalThreadHolder.getUserId().toString(); // 预先获取用户ID
        parentComments.forEach(parentComment -> {
            parentComment.setUpvoteFlag(isUserUpvote(parentComment.getUpvoteList(), userId));
            parentComment.setUpvoteCount(countVotes(parentComment.getUpvoteList()));
            Optional.ofNullable(parentComment.getCommentChildVOS())
                    .orElse(Collections.emptyList())
                    .forEach(child -> {
                        child.setUpvoteFlag(isUserUpvote(child.getUpvoteList(), userId));
                        child.setUpvoteCount(countVotes(child.getUpvoteList()));
                    });
        });
    }

    /**
     * 判断用户是否已点赞
     *
     * @param voteStr 点赞用户ID字符串（逗号分隔）
     * @param userId  用户ID
     * @return 是否已点赞
     */
    private boolean isUserUpvote(String voteStr, String userId) {
        return Optional.ofNullable(voteStr)
                .map(s -> Arrays.asList(s.split(",")))
                .orElse(Collections.emptyList())
                .contains(userId);
    }

    /**
     * 计算点赞数
     *
     * @param voteStr 点赞用户ID字符串（逗号分隔）
     * @return 点赞数
     */
    private int countVotes(String voteStr) {
        return Optional.ofNullable(voteStr)
                .map(s -> s.split(",").length)
                .orElse(0);
    }

    /**
     * 分页查询评论
     *
     * @return Result<String>
     */
    @Override
    public Result<Object> query(EvaluationsQueryDto evaluationsQueryDto) {
        List<CommentChildVO> list = evaluationsMapper.query(evaluationsQueryDto);
        Integer totalPage = evaluationsMapper.queryCount(evaluationsQueryDto);
        return PageResult.success(list, totalPage);
    }

    /**
     * 批量删除评论数据
     *
     * @return Result<String>
     */
    @Override
    public Result<Object> batchDelete(List<Integer> ids) {
        evaluationsMapper.batchDelete(ids);
        return ApiResult.success();
    }

    /**
     * 评论删除
     *
     * @return Result<String>
     */
    @Override
    public Result<String> delete(Integer id) {
        ArrayList<Integer> ids = new ArrayList<>();
        ids.add(id);
        evaluationsMapper.batchDelete(ids);
        return ApiResult.success();
    }

    /**
     * 评论修改
     *
     * @return Result<Map < String, Object>>
     */
    @Override
    public Result<Map<String, Object>> update(Evaluations evaluations) {
        EvaluationsQueryDto evaluationsQueryDto = new EvaluationsQueryDto();
        evaluationsQueryDto.setId(evaluations.getId());
        List<CommentChildVO> commentChildVOS = evaluationsMapper.query(evaluationsQueryDto);
        if (commentChildVOS.isEmpty()) {
            return ApiResult.success();
        }
        CommentChildVO commentChildVO = commentChildVOS.get(0);
        String upvoteList = commentChildVO.getUpvoteList();
        String userId = String.valueOf(LocalThreadHolder.getUserId());
        upvoteList = (upvoteList == null || upvoteList.isEmpty()) ? userId : toggleUpvote(upvoteList, userId);
        Evaluations evaluationsUpdate = new Evaluations();
        evaluationsUpdate.setId(evaluations.getId());
        if (upvoteList.contains(userId)) {
            evaluationsUpdate.setUpvoteList(upvoteList);
        }
        evaluationsUpdate.setUpvoteList(upvoteList);
        evaluationsMapper.update(evaluationsUpdate);
        Map<String, Object> result = new HashMap<>();
        // 更新之后的点赞量
        result.put("num", upvoteList.split(",").length);
        // 包含，本次为点赞，反之为取消点赞
        result.put("flag", upvoteList.contains(userId));
        MessageQueryDto messageQueryDto = new MessageQueryDto();
        messageQueryDto.setMessageType(MessageType.EVALUATIONS_BY_UPVOTE.getType());
        messageQueryDto.setUserId(LocalThreadHolder.getUserId());
        Integer exitUpvoteCount = messageMapper.queryCount(messageQueryDto);
        // 两种情况不做转发:1. 自己点赞自己的;2. 已经转发过的,即点赞一次\取消点赞一次\点赞一次
        if (!Objects.equals(LocalThreadHolder.getUserId(), commentChildVO.getUserId()) &&
                exitUpvoteCount == 0) {
            // 点赞转发
            Message message = new Message();
            message.setSenderId(LocalThreadHolder.getUserId());
            message.setReceiverId(commentChildVO.getUserId());
            message.setContent(commentChildVO.getContent());
            messageService.evaluationsUpvoteSave(message);
        }
        return ApiResult.success(result);
    }

    /**
     * 点赞字符串处理
     *
     * @param upvoteList 点赞字符串
     * @param userId     用户ID
     */
    public String toggleUpvote(String upvoteList, String userId) {
        // 将 upvoteList 转换为 Set 集合，方便进行添加和删除操作，并且自动去重
        Set<String> upvoteSet = new HashSet<>();
        if (!upvoteList.isEmpty()) {
            upvoteSet.addAll(Arrays.asList(upvoteList.split(",")));
        }
        System.out.println(upvoteSet);
        if (upvoteSet.contains(userId)) { // 取消点赞
            upvoteSet.remove(userId);
        } else { // 点赞
            upvoteSet.add(userId);
        }
        // 将 Set 集合转换回字符串并更新 upvoteList
        return String.join(",", upvoteSet);
    }

}