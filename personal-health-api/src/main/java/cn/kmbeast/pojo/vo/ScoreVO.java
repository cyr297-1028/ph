package cn.kmbeast.pojo.vo;

import cn.kmbeast.pojo.entity.Score;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 评分VO类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScoreVO extends Score {

    /**
     * 用户名
     */
    private String userName;
    /**
     * 用户的头像
     */
    private String userAvatar;

}
