package cn.kmbeast.service.impl;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.mapper.HealthModelConfigMapper;
import cn.kmbeast.mapper.UserHealthMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.PageResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.importer.UserHealthExcelDTO;
import cn.kmbeast.pojo.dto.query.base.QueryDto;
import cn.kmbeast.pojo.dto.query.extend.HealthModelConfigQueryDto;
import cn.kmbeast.pojo.dto.query.extend.UserHealthQueryDto;
import cn.kmbeast.pojo.em.IsReadEnum;
import cn.kmbeast.pojo.em.MessageType;
import cn.kmbeast.pojo.entity.HealthModelConfig;
import cn.kmbeast.pojo.entity.Message;
import cn.kmbeast.pojo.entity.UserHealth;
import cn.kmbeast.pojo.vo.ChartVO;
import cn.kmbeast.pojo.vo.HealthModelConfigVO;
import cn.kmbeast.pojo.vo.UserHealthVO;
import cn.kmbeast.service.MessageService;
import cn.kmbeast.service.UserHealthService;
import cn.kmbeast.utils.DateUtil;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户健康记录业务逻辑实现
 */
@Service
public class UserHealthServiceImpl implements UserHealthService {

    @Resource
    private UserHealthMapper userHealthMapper;
    @Resource
    private HealthModelConfigMapper healthModelConfigMapper;
    @Resource
    private MessageService messageService;

    /**
     * 导出带有下拉选项的 Excel 模板
     */
    @Override
    public void exportTemplate(HttpServletResponse response) {
        try {
            HealthModelConfigQueryDto queryDto = new HealthModelConfigQueryDto();
            List<HealthModelConfigVO> configs = healthModelConfigMapper.query(queryDto);
            List<String> modelNames = configs.stream()
                    .map(HealthModelConfigVO::getName)
                    .collect(Collectors.toList());

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("健康记录导入模板", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            EasyExcel.write(response.getOutputStream(), UserHealthExcelDTO.class)
                    .registerWriteHandler(new DropdownWriteHandler(modelNames))
                    .sheet("导入模板")
                    .doWrite(new ArrayList<>());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 一键导入健康记录
     */
    @Override
    public Result<Void> importData(MultipartFile file) {
        try {
            List<UserHealthExcelDTO> list = EasyExcel.read(file.getInputStream())
                    .head(UserHealthExcelDTO.class)
                    .sheet()
                    .doReadSync();

            if (CollectionUtils.isEmpty(list)) {
                return ApiResult.error("文件内容为空");
            }

            Integer userId = LocalThreadHolder.getUserId();
            HealthModelConfigQueryDto queryDto = new HealthModelConfigQueryDto();
            List<HealthModelConfigVO> configs = healthModelConfigMapper.query(queryDto);
            Map<String, Integer> configMap = configs.stream()
                    .collect(Collectors.toMap(HealthModelConfigVO::getName, HealthModelConfigVO::getId, (k1, k2) -> k1));

            List<UserHealth> saveList = new ArrayList<>();

            for (UserHealthExcelDTO dto : list) {
                Integer configId = configMap.get(dto.getModelName());
                if (configId == null) {
                    continue;
                }
                UserHealth userHealth = new UserHealth();
                userHealth.setUserId(userId);
                userHealth.setHealthModelConfigId(configId);

                if (dto.getValue() != null) {
                    userHealth.setValue(String.valueOf(dto.getValue()));
                }

                userHealth.setCreateTime(dto.getCreateTime() != null ?
                        DateUtil.dateToLocalDateTime(dto.getCreateTime()) : LocalDateTime.now());

                saveList.add(userHealth);
            }

            if (!CollectionUtils.isEmpty(saveList)) {
                this.save(saveList);
            }
            return ApiResult.success();

        } catch (IOException e) {
            e.printStackTrace();
            return ApiResult.error("文件解析失败");
        }
    }

    @Override
    public Result<Void> save(List<UserHealth> userHealths) {
        dealMessage(userHealths);
        dealRole(userHealths);
        userHealthMapper.batchSave(userHealths);
        return ApiResult.success();
    }

    public void dealRole(List<UserHealth> userHealths) {
        LocalDateTime nowTime = LocalDateTime.now();
        Integer userId = LocalThreadHolder.getUserId();
        userHealths.forEach(userHealth -> {
            userHealth.setUserId(userId);
            if (userHealth.getCreateTime() == null) {
                userHealth.setCreateTime(nowTime);
            }
        });
    }

    /**
     * 处理异常指标通知
     */
    private void dealMessage(List<UserHealth> userHealths) {
        List<Message> messageList = new ArrayList<>();
        userHealths.forEach(userHealth -> {
            // 如果时间为空，补全当前时间
            if (userHealth.getCreateTime() == null) {
                userHealth.setCreateTime(LocalDateTime.now());
            }
            Integer healthModelConfigId = userHealth.getHealthModelConfigId();
            HealthModelConfigQueryDto queryDto = new HealthModelConfigQueryDto();
            queryDto.setId(healthModelConfigId);
            List<HealthModelConfigVO> healthModelConfigs = healthModelConfigMapper.query(queryDto);

            if (!CollectionUtils.isEmpty(healthModelConfigs)) {
                HealthModelConfig healthModelConfig = healthModelConfigs.get(0);
                String valueRange = healthModelConfig.getValueRange();

                // 关键修改：判断阈值是否存在且格式正确
                if (StringUtils.hasText(valueRange) && valueRange.contains(",")) {
                    String[] values = valueRange.split(",");
                    try {
                        double mixValue = Double.parseDouble(values[0]);
                        double maxValue = Double.parseDouble(values[1]);
                        double value = Double.parseDouble(String.valueOf(userHealth.getValue()));

                        // 超出范围则预警
                        if (value < mixValue || value > maxValue) {
                            Message message = sendMessage(healthModelConfig, userHealth);
                            messageList.add(message);
                        }
                    } catch (NumberFormatException e) {
                        // 忽略格式错误
                    }
                }
            }
        });
        if (!CollectionUtils.isEmpty(messageList)) {
            messageService.dataWordSave(messageList);
        }
    }

    private Message sendMessage(HealthModelConfig healthModelConfig, UserHealth userHealth) {
        Message message = new Message();
        message.setMessageType(MessageType.DATA_MESSAGE.getType());
        message.setCreateTime(LocalDateTime.now());
        message.setIsRead(IsReadEnum.READ_NO.getStatus());
        message.setReceiverId(LocalThreadHolder.getUserId());
        message.setContent("你记录的【" + healthModelConfig.getName() + "】超标了，正常值范围:[" + healthModelConfig.getValueRange() + "]，请注意休息。必要时请就医!");
        return message;
    }

    @Override public Result<Void> batchDelete(List<Long> ids) { userHealthMapper.batchDelete(ids); return ApiResult.success(); }
    @Override public Result<Void> update(UserHealth userHealth) { userHealthMapper.update(userHealth); return ApiResult.success(); }
    @Override public Result<List<UserHealthVO>> query(UserHealthQueryDto dto) { return PageResult.success(userHealthMapper.query(dto), userHealthMapper.queryCount(dto)); }
    @Override public Result<List<ChartVO>> daysQuery(Integer day) {
        QueryDto queryDto = DateUtil.startAndEndTime(day);
        UserHealthQueryDto userHealthQueryDto = new UserHealthQueryDto();
        userHealthQueryDto.setStartTime(queryDto.getStartTime());
        userHealthQueryDto.setEndTime(queryDto.getEndTime());
        List<UserHealthVO> userHealthVOS = userHealthMapper.query(userHealthQueryDto);
        List<LocalDateTime> localDateTimes = userHealthVOS.stream().map(UserHealthVO::getCreateTime).collect(Collectors.toList());
        List<ChartVO> chartVOS = DateUtil.countDatesWithinRange(day, localDateTimes);
        return ApiResult.success(chartVOS);
    }

    public static class DropdownWriteHandler implements SheetWriteHandler {
        private final List<String> options;
        public DropdownWriteHandler(List<String> options) { this.options = options; }
        @Override public void beforeSheetCreate(WriteWorkbookHolder h, WriteSheetHolder s) {}
        @Override public void afterSheetCreate(WriteWorkbookHolder h, WriteSheetHolder s) {
            if (options == null || options.isEmpty()) return;
            Workbook workbook = h.getWorkbook();
            Sheet sheet = s.getSheet();
            String hiddenSheetName = "HiddenModelOptions";
            Sheet hiddenSheet = workbook.createSheet(hiddenSheetName);
            workbook.setSheetHidden(workbook.getSheetIndex(hiddenSheet), true);
            for (int i = 0; i < options.size(); i++) {
                hiddenSheet.createRow(i).createCell(0).setCellValue(options.get(i));
            }
            Name categoryName = workbook.createName();
            categoryName.setNameName("ModelNameList");
            categoryName.setRefersToFormula(hiddenSheetName + "!$A$1:$A$" + options.size());
            DataValidationHelper helper = sheet.getDataValidationHelper();
            DataValidationConstraint constraint = helper.createFormulaListConstraint("ModelNameList");
            CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, 0, 0);
            DataValidation validation = helper.createValidation(constraint, addressList);
            validation.createErrorBox("输入错误", "请从下拉列表中选择有效的健康模型名称");
            validation.setShowErrorBox(true);
            sheet.addValidationData(validation);
        }
    }
}