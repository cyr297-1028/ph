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
