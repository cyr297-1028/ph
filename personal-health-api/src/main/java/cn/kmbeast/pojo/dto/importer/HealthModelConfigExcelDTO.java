package cn.kmbeast.pojo.dto.importer;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class HealthModelConfigExcelDTO {
    @ExcelProperty("配置名")
    private String name;

    @ExcelProperty("单位")
    private String unit;

    @ExcelProperty("符号")
    private String symbol;

    @ExcelProperty("阈值")
    private String valueRange;

    @ExcelProperty("简介")
    private String detail;
}