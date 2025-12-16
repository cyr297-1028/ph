package cn.kmbeast.pojo.dto.importer;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 用户健康记录导入实体类
 */
@Data
public class UserHealthExcelDTO {
    @ExcelProperty("配置名")
    private String modelName;

    @ExcelProperty("数值")
    private Double value;

    @ExcelProperty("记录时间")
    private Date createTime;
}