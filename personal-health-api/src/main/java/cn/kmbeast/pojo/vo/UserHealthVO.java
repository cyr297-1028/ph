package cn.kmbeast.pojo.vo;

import cn.kmbeast.pojo.entity.UserHealth;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserHealthVO extends UserHealth {
    /**
     * 健康模型名称
     */
    private String name;
    /**
     * 健康模型单位
     */
    private String unit;
    /**
     * 健康模型符号
     */
    private String symbol;
    /**
     * 健康模型阈值
     */
    private String valueRange;
    /**
     * 标签/分类
     */
    private String tag;
}