package cn.kmbeast.pojo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 行为互动表实体类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Score {

    /**
     * 主键ID
     */
    private Integer id;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 物品ID
     */
    private Integer articleId;

    /**
     * 评分
     */
    private Integer total;

    /**
     * 评分时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
