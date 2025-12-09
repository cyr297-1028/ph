package cn.kmbeast.service;

import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.ScoreQueryDto;
import cn.kmbeast.pojo.entity.Score;
import cn.kmbeast.pojo.vo.ScoreVO;

import java.util.List;

/**
 * 评分业务逻辑接口
 */
public interface ScoreService {

    Result<Void> save(Score score);

    Result<List<ScoreVO>> query(ScoreQueryDto scoreQueryDto);
}
