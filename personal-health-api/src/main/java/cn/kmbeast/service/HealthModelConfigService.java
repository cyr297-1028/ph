package cn.kmbeast.service;

import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.HealthModelConfigQueryDto;
import cn.kmbeast.pojo.entity.HealthModelConfig;
import cn.kmbeast.pojo.vo.HealthModelConfigVO;
import org.springframework.web.multipart.MultipartFile; // 记得导入

import javax.servlet.http.HttpServletResponse;
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

    Result<Void> batchSave(List<HealthModelConfig> list, Boolean isGlobal);

    /**
     * Excel 文件导入模型
     * @param file Excel文件
     * @param isGlobal 是否为全局模型
     * @return Result
     */
    Result<Void> importData(MultipartFile file, Boolean isGlobal); // 增加 Boolean isGlobal 参数

    /**
     * 导出 Excel 模板
     * @param response 响应对象
     */
    void exportTemplate(HttpServletResponse response);
}