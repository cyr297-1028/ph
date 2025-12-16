package cn.kmbeast.service.impl;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.mapper.HealthModelConfigMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.PageResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.importer.HealthModelConfigExcelDTO;
import cn.kmbeast.pojo.dto.query.extend.HealthModelConfigQueryDto;
import cn.kmbeast.pojo.entity.HealthModelConfig;
import cn.kmbeast.pojo.vo.HealthModelConfigVO;
import cn.kmbeast.service.HealthModelConfigService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 健康模型业务逻辑实现
 */
@Service
public class HealthModelConfigServiceImpl implements HealthModelConfigService {

    @Resource
    private HealthModelConfigMapper healthModelConfigMapper;

    /**
     * 导出带有下拉选项（现有模型名称）的 Excel 模板
     */
    @Override
    public void exportTemplate(HttpServletResponse response) {
        try {
            // 1. 查询所有现有的健康模型名称
            HealthModelConfigQueryDto queryDto = new HealthModelConfigQueryDto();
            List<HealthModelConfigVO> configs = healthModelConfigMapper.query(queryDto);
            List<String> modelNames = configs.stream()
                    .map(HealthModelConfigVO::getName)
                    .distinct()
                    .collect(Collectors.toList());

            // 2. 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("健康模型导入模板", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            // 3. 生成示例数据
            List<HealthModelConfigExcelDTO> list = new ArrayList<>();
            HealthModelConfigExcelDTO example = new HealthModelConfigExcelDTO();
            example.setName("示例:尿酸");
            example.setUnit("μmol/L");
            example.setSymbol("UA");
            example.setValueRange("149,416");
            example.setDetail("导入前请删除此行");
            list.add(example);

            // 4. 写入 Excel
            EasyExcel.write(response.getOutputStream(), HealthModelConfigExcelDTO.class)
                    .registerWriteHandler(new DropdownWriteHandler(modelNames))
                    .sheet("模板")
                    .doWrite(list);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从 Excel 文件一键导入健康模型
     */
    @Override
    public Result<Void> importData(MultipartFile file, Boolean isGlobal) {
        try {
            List<HealthModelConfigExcelDTO> list = EasyExcel.read(file.getInputStream())
                    .head(HealthModelConfigExcelDTO.class)
                    .sheet()
                    .doReadSync();

            if (CollectionUtils.isEmpty(list)) {
                return ApiResult.error("文件内容为空");
            }

            List<HealthModelConfig> saveList = new ArrayList<>();
            Integer userId = LocalThreadHolder.getUserId();

            for (HealthModelConfigExcelDTO dto : list) {
                // 仅校验必填项 (配置名)，不再强制校验阈值，允许空阈值
                if (!StringUtils.hasText(dto.getName())) {
                    continue;
                }
                if (dto.getName().startsWith("示例:")) {
                    continue;
                }

                HealthModelConfig config = new HealthModelConfig();
                config.setUserId(userId);
                config.setName(dto.getName());
                config.setUnit(dto.getUnit());
                config.setSymbol(dto.getSymbol());
                config.setDetail(dto.getDetail());
                config.setCover("");

                // 处理阈值：允许为空，若有值则处理中文逗号
                String range = dto.getValueRange();
                if (StringUtils.hasText(range) && range.contains("，")) {
                    range = range.replace("，", ",");
                }
                config.setValueRange(range);

                // 设置是否全局
                config.setIsGlobal(isGlobal);

                saveList.add(config);
            }

            if (!CollectionUtils.isEmpty(saveList)) {
                healthModelConfigMapper.batchSave(saveList);
            }
            return ApiResult.success();

        } catch (IOException e) {
            e.printStackTrace();
            return ApiResult.error("文件解析失败");
        }
    }

    @Override
    public Result<Void> batchSave(List<HealthModelConfig> list, Boolean isGlobal) {
        if (list == null || list.isEmpty()) {
            return ApiResult.error("导入数据不能为空");
        }
        Integer currentUserId = LocalThreadHolder.getUserId();
        List<HealthModelConfig> validList = new ArrayList<>();
        for (HealthModelConfig config : list) {
            // 不再校验 valueRange
            if (!StringUtils.hasText(config.getName()) ||
                    !StringUtils.hasText(config.getUnit()) ||
                    !StringUtils.hasText(config.getSymbol())) {
                continue;
            }
            // 处理中文逗号
            if (StringUtils.hasText(config.getValueRange()) && config.getValueRange().contains("，")) {
                config.setValueRange(config.getValueRange().replace("，", ","));
            }
            config.setUserId(currentUserId);
            config.setIsGlobal(isGlobal);
            validList.add(config);
        }
        if (validList.isEmpty()) {
            return ApiResult.error("没有有效的模型数据可导入");
        }
        healthModelConfigMapper.batchSave(validList);
        return ApiResult.success();
    }

    @Override
    public Result<Void> save(HealthModelConfig healthModelConfig) {
        if (!StringUtils.hasText(healthModelConfig.getName())) return ApiResult.error("模型名不能为空");
        if (!StringUtils.hasText(healthModelConfig.getUnit())) return ApiResult.error("模型单位不能为空");
        if (!StringUtils.hasText(healthModelConfig.getSymbol())) return ApiResult.error("模型符号不能为空");

        // 已移除：if (!StringUtils.hasText(healthModelConfig.getValueRange())) return ApiResult.error("请输入阈值");

        if (StringUtils.hasText(healthModelConfig.getValueRange()) && healthModelConfig.getValueRange().contains("，")) {
            healthModelConfig.setValueRange(healthModelConfig.getValueRange().replace("，", ","));
        }
        healthModelConfig.setUserId(LocalThreadHolder.getUserId());
        healthModelConfigMapper.save(healthModelConfig);
        return ApiResult.success();
    }

    @Override
    public Result<Void> batchDelete(List<Long> ids) {
        healthModelConfigMapper.batchDelete(ids);
        return ApiResult.success();
    }

    @Override
    public Result<Void> update(HealthModelConfig healthModelConfig) {
        healthModelConfigMapper.update(healthModelConfig);
        return ApiResult.success();
    }

    @Override
    public Result<List<HealthModelConfigVO>> modelList() {
        HealthModelConfigQueryDto dto = new HealthModelConfigQueryDto();
        dto.setUserId(LocalThreadHolder.getUserId());
        List<HealthModelConfigVO> list1 = healthModelConfigMapper.query(dto);
        dto.setUserId(null); dto.setIsGlobal(true);
        List<HealthModelConfigVO> list2 = healthModelConfigMapper.query(dto);
        List<HealthModelConfigVO> all = new ArrayList<>();
        all.addAll(list1); all.addAll(list2);
        return ApiResult.success(all);
    }

    @Override
    public Result<List<HealthModelConfigVO>> query(HealthModelConfigQueryDto dto) {
        List<HealthModelConfigVO> modelConfigs = healthModelConfigMapper.query(dto);
        Integer totalCount = healthModelConfigMapper.queryCount(dto);
        return PageResult.success(modelConfigs, totalCount);
    }

    /**
     * 内部类：处理 Excel 下拉列表
     */
    public static class DropdownWriteHandler implements SheetWriteHandler {
        private final List<String> options;
        public DropdownWriteHandler(List<String> options) { this.options = options; }
        @Override public void beforeSheetCreate(WriteWorkbookHolder h, WriteSheetHolder s) {}
        @Override public void afterSheetCreate(WriteWorkbookHolder h, WriteSheetHolder s) {
            if (options == null || options.isEmpty()) return;
            Workbook workbook = h.getWorkbook();
            Sheet sheet = s.getSheet();
            String hiddenSheetName = "HiddenModelOptions";
            Sheet hiddenSheet = workbook.getSheet(hiddenSheetName);
            if (hiddenSheet == null) {
                hiddenSheet = workbook.createSheet(hiddenSheetName);
                workbook.setSheetHidden(workbook.getSheetIndex(hiddenSheet), true);
                for (int i = 0; i < options.size(); i++) {
                    hiddenSheet.createRow(i).createCell(0).setCellValue(options.get(i));
                }
            }
            Name categoryName = workbook.getName("ModelNameList");
            if (categoryName == null) {
                categoryName = workbook.createName();
                categoryName.setNameName("ModelNameList");
                categoryName.setRefersToFormula(hiddenSheetName + "!$A$1:$A$" + options.size());
            }
            DataValidationHelper helper = sheet.getDataValidationHelper();
            DataValidationConstraint constraint = helper.createFormulaListConstraint("ModelNameList");
            CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, 0, 0);
            DataValidation validation = helper.createValidation(constraint, addressList);
            // 允许输入新值（非严格校验），方便管理员创建新模型
            validation.createErrorBox("提示", "您输入了一个新的模型名称，将创建一个新的健康模型。");
            validation.setShowErrorBox(false);
            sheet.addValidationData(validation);
        }
    }
}