package cn.kmbeast.service.impl;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.mapper.HealthModelConfigMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.PageResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.HealthModelConfigQueryDto;
import cn.kmbeast.pojo.entity.HealthModelConfig;
import cn.kmbeast.pojo.vo.HealthModelConfigVO;
import cn.kmbeast.service.HealthModelConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 健康模型业务逻辑实现
 */
@Service
public class HealthModelConfigServiceImpl implements HealthModelConfigService {

    @Resource
    private HealthModelConfigMapper healthModelConfigMapper;

    @Override
    public Result<Void> batchSave(List<HealthModelConfig> list, Boolean isGlobal) {
        if (list == null || list.isEmpty()) {
            return ApiResult.error("导入数据不能为空");
        }

        // 获取当前操作人的ID
        Integer currentUserId = LocalThreadHolder.getUserId();

        // 预处理数据
        // 建议使用迭代器或者新集合过滤掉不合法的数据，防止空指针或插入失败
        List<HealthModelConfig> validList = new ArrayList<>();

        for (HealthModelConfig config : list) {
            // 简单校验必填项 (可根据需求决定是跳过还是直接报错返回)
            if (!StringUtils.hasText(config.getName()) ||
                    !StringUtils.hasText(config.getUnit()) ||
                    !StringUtils.hasText(config.getSymbol()) ||
                    !StringUtils.hasText(config.getValueRange())) {
                continue; // 跳过不完整的数据
            }

            // 处理中文逗号兼容性
            if (config.getValueRange().contains("，")) {
                config.setValueRange(config.getValueRange().replace("，", ","));
            }

            // 设置关键字段
            config.setUserId(currentUserId);
            config.setIsGlobal(isGlobal); // 根据传入参数决定是否全局

            validList.add(config);
        }

        if (validList.isEmpty()) {
            return ApiResult.error("没有有效的模型数据可导入");
        }

        // 调用Mapper进行批量插入
        healthModelConfigMapper.batchSave(validList);
        return ApiResult.success();
    }

    /**
     * 健康模型新增
     *
     * @param healthModelConfig 参数
     * @return Result<Void>
     */
    @Override
    public Result<Void> save(HealthModelConfig healthModelConfig) {
        if (!StringUtils.hasText(healthModelConfig.getName())) {
            return ApiResult.error("模型名不能为空");
        }
        if (!StringUtils.hasText(healthModelConfig.getUnit())) {
            return ApiResult.error("模型单位不能为空");
        }
        if (!StringUtils.hasText(healthModelConfig.getSymbol())) {
            return ApiResult.error("模型符号不能为空");
        }
        if (!StringUtils.hasText(healthModelConfig.getValueRange())) {
            return ApiResult.error("请输入阈值");
        }
        // 如果是中文逗号，做兼容
        if (healthModelConfig.getValueRange().contains("，")) {
            String replace = healthModelConfig.getValueRange().replace("，", ",");
            healthModelConfig.setValueRange(replace);
        }
        // 将用户的ID设置上
        healthModelConfig.setUserId(LocalThreadHolder.getUserId());
        healthModelConfigMapper.save(healthModelConfig);
        return ApiResult.success();
    }

    /**
     * 健康模型删除
     *
     * @param ids 参数
     * @return Result<Void>
     */
    @Override
    public Result<Void> batchDelete(List<Long> ids) {
        healthModelConfigMapper.batchDelete(ids);
        return ApiResult.success();
    }

    /**
     * 健康模型修改
     *
     * @param healthModelConfig 参数
     * @return Result<Void>
     */
    @Override
    public Result<Void> update(HealthModelConfig healthModelConfig) {
        healthModelConfigMapper.update(healthModelConfig);
        return ApiResult.success();
    }

    /**
     * 查询用户自己配置的模型及全局模型
     *
     * @return Result<List < HealthModelConfigVO>>
     */
    @Override
    public Result<List<HealthModelConfigVO>> modelList() {
        HealthModelConfigQueryDto healthModelConfigQueryDto = new HealthModelConfigQueryDto();
        healthModelConfigQueryDto.setUserId(LocalThreadHolder.getUserId());
        List<HealthModelConfigVO> modelConfigs = healthModelConfigMapper.query(healthModelConfigQueryDto);
        healthModelConfigQueryDto.setUserId(null);
        healthModelConfigQueryDto.setIsGlobal(true);
        List<HealthModelConfigVO> modelConfigsGlobal = healthModelConfigMapper.query(healthModelConfigQueryDto);
        List<HealthModelConfigVO> modelAll = new ArrayList<>();
        modelAll.addAll(modelConfigs);
        modelAll.addAll(modelConfigsGlobal);
        return ApiResult.success(modelAll);
    }

    /**
     * 健康模型查询
     *
     * @param healthModelConfigQueryDto 查询参数
     * @return Result<List < HealthModelConfigVO>>
     */
    @Override
    public Result<List<HealthModelConfigVO>> query(HealthModelConfigQueryDto healthModelConfigQueryDto) {
        List<HealthModelConfigVO> modelConfigs = healthModelConfigMapper.query(healthModelConfigQueryDto);
        Integer totalCount = healthModelConfigMapper.queryCount(healthModelConfigQueryDto);
        return PageResult.success(modelConfigs, totalCount);
    }

}
