package cn.kmbeast.pojo.dto.query.extend;

import cn.kmbeast.pojo.dto.query.base.QueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评分查询
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ScoreQueryDto extends QueryDto {
    private Integer userId;
    private Integer articleId;
}
