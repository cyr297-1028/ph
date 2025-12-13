package cn.kmbeast.service;

import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.HealthModelConfigQueryDto;
import cn.kmbeast.pojo.entity.HealthModelConfig;
import cn.kmbeast.pojo.vo.ChartVO;
import cn.kmbeast.pojo.vo.HealthModelConfigVO;

import java.util.List;

/**
 * 健康模型业务逻辑接口
 */
public interface HealthModelConfigService {

    Result<Void> save(HealthModelConfig healthModelConfig);

    Result<Void> batchDelete(List<Long> ids);

    Result<Void> update(HealthModelConfig healthModelConfig);

    Result<List<HealthModelConfigVO>> query(HealthModelConfigQueryDto healthModelConfigQueryDto);

    Result<List<HealthModelConfigVO>> modelList();

    /**
     * 批量新增健康模型
     * @param list 模型数据列表
     * @param isGlobal 是否为全局模型
     * @return Result<Void>
     */
    Result<Void> batchSave(List<HealthModelConfig> list, Boolean isGlobal);

}
