package cn.kmbeast.mapper;

import cn.kmbeast.pojo.dto.query.extend.ScoreQueryDto;
import cn.kmbeast.pojo.dto.query.extend.TagsQueryDto;
import cn.kmbeast.pojo.entity.Score;
import cn.kmbeast.pojo.entity.Tags;
import cn.kmbeast.pojo.vo.ScoreVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评分持久化接口
 */
@Mapper
public interface ScoreMapper {

    void save(Score score);

    List<ScoreVO> query(ScoreQueryDto scoreQueryDto);

}
