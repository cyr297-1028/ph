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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 健康模型业务逻辑实现
 */
@Service
public class HealthModelConfigServiceImpl implements HealthModelConfigService {

    @Resource
    private HealthModelConfigMapper healthModelConfigMapper;

    /**
     * 导出带有下拉选项的 Excel 模板
     * 【修复关键】增加 .inMemory(true) 防止文件损坏
     */
    @Override
    public void exportTemplate(HttpServletResponse response) {
        try {
            HealthModelConfigQueryDto queryDto = new HealthModelConfigQueryDto();
            List<HealthModelConfigVO> configs = healthModelConfigMapper.query(queryDto);
            List<String> modelNames = configs.stream()
                    .map(HealthModelConfigVO::getName)
                    .distinct()
                    .collect(Collectors.toList());

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("健康模型导入模板", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            List<HealthModelConfigExcelDTO> list = new ArrayList<>();
            HealthModelConfigExcelDTO example = new HealthModelConfigExcelDTO();
            example.setName("示例:尿酸");
            example.setTag("肾功能");
            example.setUnit("μmol/L");
            example.setSymbol("UA");
            example.setValueRange("149,416");
            example.setDetail("导入前请删除此行");
            list.add(example);

            EasyExcel.write(response.getOutputStream(), HealthModelConfigExcelDTO.class)
                    .inMemory(true) // <--- 关键修复！强制使用内存模式，解决 Excel 打开报错问题
                    .registerWriteHandler(new DropdownWriteHandler(modelNames))
                    .sheet("模板")
                    .doWrite(list);

        } catch (Exception e) {
            e.printStackTrace();
            // 如果出错，重置 response，防止写入损坏的流 (可选)
            try {
                response.reset();
                response.setContentType("application/json");
                response.setCharacterEncoding("utf-8");
                response.getWriter().println("{\"code\": 500, \"msg\": \"模板下载失败: " + e.getMessage() + "\"}");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    // ... (中间的 importData, save, update 等方法保持不变，直接复制您现有的即可) ...
    // 为了节省篇幅，这里省略中间未修改的代码，请保留您原有的 importData 等逻辑
    // 重点是下方的 DropdownWriteHandler 内部类

    @Override
    public Result<Void> importData(MultipartFile file, Boolean isGlobal) {
        try {
            List<HealthModelConfigExcelDTO> list = EasyExcel.read(file.getInputStream()).head(HealthModelConfigExcelDTO.class).sheet().doReadSync();
            if (CollectionUtils.isEmpty(list)) return ApiResult.error("文件内容为空");

            HealthModelConfigQueryDto queryDto = new HealthModelConfigQueryDto();
            queryDto.setIsGlobal(isGlobal);
            if (!isGlobal) queryDto.setUserId(LocalThreadHolder.getUserId());
            List<HealthModelConfigVO> existingModels = healthModelConfigMapper.query(queryDto);
            Set<String> existingNames = existingModels.stream().map(HealthModelConfigVO::getName).collect(Collectors.toSet());

            List<HealthModelConfig> saveList = new ArrayList<>();
            Integer userId = LocalThreadHolder.getUserId();
            Set<String> excelInternalNames = new HashSet<>();

            for (HealthModelConfigExcelDTO dto : list) {
                String name = dto.getName();
                if (!StringUtils.hasText(name) || name.startsWith("示例:")) continue;
                if (existingNames.contains(name) || excelInternalNames.contains(name)) continue;

                excelInternalNames.add(name);
                HealthModelConfig config = new HealthModelConfig();
                config.setUserId(userId);
                config.setName(name);
                config.setTag(dto.getTag());
                config.setUnit(dto.getUnit());
                config.setSymbol(dto.getSymbol());
                config.setDetail(dto.getDetail());
                String range = dto.getValueRange();
                if (StringUtils.hasText(range)) config.setValueRange(range.replace("，", ","));
                config.setIsGlobal(isGlobal);
                saveList.add(config);
            }

            if (!CollectionUtils.isEmpty(saveList)) {
                healthModelConfigMapper.batchSave(saveList);
                return ApiResult.success("导入成功，已跳过重复数据 " + (list.size() - saveList.size()) + " 条");
            }
            return ApiResult.success("没有新数据导入");
        } catch (IOException e) {
            e.printStackTrace();
            return ApiResult.error("文件解析失败");
        }
    }

    @Override
    public Result<Void> batchSave(List<HealthModelConfig> list, Boolean isGlobal) {
        if (list == null || list.isEmpty()) return ApiResult.error("数据为空");
        Integer currentUserId = LocalThreadHolder.getUserId();
        List<HealthModelConfig> validList = new ArrayList<>();
        for (HealthModelConfig config : list) {
            if (!StringUtils.hasText(config.getName())) continue;
            if (StringUtils.hasText(config.getValueRange())) config.setValueRange(config.getValueRange().replace("，", ","));
            config.setUserId(currentUserId);
            config.setIsGlobal(isGlobal);
            validList.add(config);
        }
        if (validList.isEmpty()) return ApiResult.error("有效数据为空");
        healthModelConfigMapper.batchSave(validList);
        return ApiResult.success();
    }

    @Override
    public Result<Void> save(HealthModelConfig healthModelConfig) {
        if (!StringUtils.hasText(healthModelConfig.getName())) return ApiResult.error("配置名不能为空");
        if (StringUtils.hasText(healthModelConfig.getValueRange())) healthModelConfig.setValueRange(healthModelConfig.getValueRange().replace("，", ","));
        healthModelConfig.setUserId(LocalThreadHolder.getUserId());
        healthModelConfigMapper.save(healthModelConfig);
        return ApiResult.success();
    }

    @Override public Result<Void> batchDelete(List<Long> ids) { healthModelConfigMapper.batchDelete(ids); return ApiResult.success(); }
    @Override public Result<Void> update(HealthModelConfig healthModelConfig) { healthModelConfigMapper.update(healthModelConfig); return ApiResult.success(); }
    @Override public Result<List<HealthModelConfigVO>> modelList() {
        HealthModelConfigQueryDto dto = new HealthModelConfigQueryDto(); dto.setUserId(LocalThreadHolder.getUserId());
        List<HealthModelConfigVO> list1 = healthModelConfigMapper.query(dto);
        dto.setUserId(null); dto.setIsGlobal(true);
        List<HealthModelConfigVO> list2 = healthModelConfigMapper.query(dto);
        List<HealthModelConfigVO> all = new ArrayList<>(list1); all.addAll(list2);
        return ApiResult.success(all);
    }
    @Override public Result<List<HealthModelConfigVO>> query(HealthModelConfigQueryDto dto) {
        return PageResult.success(healthModelConfigMapper.query(dto), healthModelConfigMapper.queryCount(dto));
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

            // 1. 创建隐藏的选项 Sheet
            String hiddenSheetName = "HiddenModelOptions";
            Sheet hiddenSheet = workbook.getSheet(hiddenSheetName);
            if (hiddenSheet == null) {
                hiddenSheet = workbook.createSheet(hiddenSheetName);
                workbook.setSheetHidden(workbook.getSheetIndex(hiddenSheet), true);
                for (int i = 0; i < options.size(); i++) {
                    hiddenSheet.createRow(i).createCell(0).setCellValue(options.get(i));
                }
            }

            // 2. 创建名称管理器 (加单引号防止Sheet名包含特殊字符导致引用失败)
            String nameName = "ModelNameList";
            Name categoryName = workbook.getName(nameName);
            if (categoryName == null) {
                categoryName = workbook.createName();
                categoryName.setNameName(nameName);
                // 【优化】加上单引号
                categoryName.setRefersToFormula("'" + hiddenSheetName + "'!$A$1:$A$" + options.size());
            }

            // 3. 应用数据验证
            DataValidationHelper helper = sheet.getDataValidationHelper();
            DataValidationConstraint constraint = helper.createFormulaListConstraint(nameName);
            CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, 0, 0);
            DataValidation validation = helper.createValidation(constraint, addressList);
            validation.createErrorBox("提示", "您输入了一个新的模型名称，将创建一个新的健康模型。");
            validation.setShowErrorBox(false);
            sheet.addValidationData(validation);
        }
    }
}