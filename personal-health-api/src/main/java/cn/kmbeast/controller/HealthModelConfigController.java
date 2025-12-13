package cn.kmbeast.controller;

import cn.kmbeast.aop.Pager;
import cn.kmbeast.aop.Protector;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.HealthModelConfigQueryDto;
import cn.kmbeast.pojo.entity.HealthModelConfig;
import cn.kmbeast.pojo.vo.HealthModelConfigVO;
import cn.kmbeast.service.HealthModelConfigService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 健康模型的 Controller
 */
@RestController
@RequestMapping(value = "/health-model-config")
public class HealthModelConfigController {

    @Resource
    private HealthModelConfigService healthModelConfigService;
    
    /**
     * 批量导入健康模型 (用户端 - 私有模型)
     *
     * @param list 模型列表
     * @return Result<Void>
     */
    @PostMapping(value = "/batchImport")
    public Result<Void> batchImport(@RequestBody List<HealthModelConfig> list) {
        // 用户导入的强制设为非全局
        return healthModelConfigService.batchSave(list, false);
    }

    /**
     * 批量导入健康模型 (管理端 - 全局模型)
     *
     * @param list 模型列表
     * @return Result<Void>
     */
    @Protector(role = "管理员") // 权限控制
    @PostMapping(value = "config/batchImport")
    public Result<Void> configBatchImport(@RequestBody List<HealthModelConfig> list) {
        // 管理员导入的设为全局
        return healthModelConfigService.batchSave(list, true);
    }

    /**
     * 健康模型新增
     *
     * @param healthModelConfig 新增数据
     * @return Result<Void> 通用响应体
     */
    @PostMapping(value = "/save")
    public Result<Void> save(@RequestBody HealthModelConfig healthModelConfig) {
        // 这是给予用户新增模型的，用户新增的模型不是全局模型
        healthModelConfig.setIsGlobal(false);
        return healthModelConfigService.save(healthModelConfig);
    }

    /**
     * 健康模型新增(管理员)
     *
     * @param healthModelConfig 新增数据
     * @return Result<Void> 通用响应体
     */
    @Protector(role = "管理员") // 加上这行注解，只有管理员有权操作这个接口
    @PostMapping(value = "config/save")
    public Result<Void> configSave(@RequestBody HealthModelConfig healthModelConfig) {
        // 设置为全局配置
        healthModelConfig.setIsGlobal(true);
        return healthModelConfigService.save(healthModelConfig);
    }

    /**
     * 健康模型删除
     *
     * @param ids 要删除的健康模型ID列表
     * @return Result<Void> 通用响应体
     */
    @PostMapping(value = "/batchDelete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        return healthModelConfigService.batchDelete(ids);
    }

    /**
     * 健康模型修改
     *
     * @param healthModelConfig 参数
     * @return Result<Void> 响应
     */
    @PutMapping(value = "/update")
    public Result<Void> update(@RequestBody HealthModelConfig healthModelConfig) {
        return healthModelConfigService.update(healthModelConfig);
    }

    /**
     * 查询用户自己配置的模型及全局模型
     *
     * @return Result<List < HealthModelConfigVO>> 通用响应
     */
    @Pager
    @PostMapping(value = "/modelList")
    public Result<List<HealthModelConfigVO>> modelList() {
        return healthModelConfigService.modelList();
    }

    /**
     * 健康模型查询
     *
     * @param healthModelConfigQueryDto 查询参数
     * @return Result<List < HealthModelConfigVO>> 通用响应
     */
    @Pager
    @PostMapping(value = "/query")
    public Result<List<HealthModelConfigVO>> query(@RequestBody HealthModelConfigQueryDto healthModelConfigQueryDto) {
        return healthModelConfigService.query(healthModelConfigQueryDto);
    }


}
