package cn.kmbeast.service.impl;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.mapper.ScoreMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.ScoreQueryDto;
import cn.kmbeast.pojo.entity.Score;
import cn.kmbeast.pojo.vo.ScoreVO;
import cn.kmbeast.service.ScoreService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * 评分业务逻辑实现
 */
@Service
public class ScoreServiceImpl implements ScoreService {

    @Resource
    private ScoreMapper scoreMapper;

    /**
     * 评分新增
     *
     * @param score 参数
     * @return Result<Void>
     */
    @Override
    public Result<Void> save(Score score) {
        score.setUserId(LocalThreadHolder.getUserId());
        score.setCreateTime(LocalDateTime.now());
        scoreMapper.save(score);
        return ApiResult.success();
    }

    /**
     * 评分查询
     *
     * @param scoreQueryDto 查询参数
     * @return Result<List < ScoreVO>>
     */
    @Override
    public Result<List<ScoreVO>> query(ScoreQueryDto scoreQueryDto) {
        List<ScoreVO> scoreVOS = scoreMapper.query(scoreQueryDto);
        return ApiResult.success(scoreVOS);
    }


}
