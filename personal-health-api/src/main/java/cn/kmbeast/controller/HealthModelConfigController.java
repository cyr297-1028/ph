package cn.kmbeast.controller;

import cn.kmbeast.aop.Pager;
import cn.kmbeast.aop.Protector;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.HealthModelConfigQueryDto;
import cn.kmbeast.pojo.entity.HealthModelConfig;
import cn.kmbeast.pojo.vo.HealthModelConfigVO;
import cn.kmbeast.service.HealthModelConfigService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
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
     * 批量导入健康模型 (用户端 - 私有模型 - JSON方式)
     */
    @PostMapping(value = "/batchImport")
    public Result<Void> batchImport(@RequestBody List<HealthModelConfig> list) {
        return healthModelConfigService.batchSave(list, false);
    }

    /**
     * 下载模型导入模板
     */
    @GetMapping(value = "/template")
    public void downloadTemplate(HttpServletResponse response) {
        healthModelConfigService.exportTemplate(response);
    }

    /**
     * 用户端：Excel 一键导入模型 (私有)
     */
    @PostMapping(value = "/import")
    public Result<Void> importData(@RequestParam("file") MultipartFile file) {
        // 传 false，表示非全局
        return healthModelConfigService.importData(file, false);
    }

    /**
     * 批量导入健康模型 (管理端 - 全局模型 - JSON方式)
     */
    @Protector(role = "管理员")
    @PostMapping(value = "config/batchImport")
    public Result<Void> configBatchImport(@RequestBody List<HealthModelConfig> list) {
        return healthModelConfigService.batchSave(list, true);
    }

    /**
     * 管理端：Excel 一键导入模型 (全局)
     */
    @Protector(role = "管理员")
    @PostMapping(value = "config/import")
    public Result<Void> configImportData(@RequestParam("file") MultipartFile file) {
        // 传 true，表示全局
        return healthModelConfigService.importData(file, true);
    }

    /**
     * 健康模型新增 (用户)
     */
    @PostMapping(value = "/save")
    public Result<Void> save(@RequestBody HealthModelConfig healthModelConfig) {
        healthModelConfig.setIsGlobal(false);
        return healthModelConfigService.save(healthModelConfig);
    }

    /**
     * 健康模型新增 (管理员)
     */
    @Protector(role = "管理员")
    @PostMapping(value = "config/save")
    public Result<Void> configSave(@RequestBody HealthModelConfig healthModelConfig) {
        healthModelConfig.setIsGlobal(true);
        return healthModelConfigService.save(healthModelConfig);
    }

    @PostMapping(value = "/batchDelete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        return healthModelConfigService.batchDelete(ids);
    }

    @PutMapping(value = "/update")
    public Result<Void> update(@RequestBody HealthModelConfig healthModelConfig) {
        return healthModelConfigService.update(healthModelConfig);
    }

    @Pager
    @PostMapping(value = "/modelList")
    public Result<List<HealthModelConfigVO>> modelList() {
        return healthModelConfigService.modelList();
    }

    @Pager
    @PostMapping(value = "/query")
    public Result<List<HealthModelConfigVO>> query(@RequestBody HealthModelConfigQueryDto healthModelConfigQueryDto) {
        return healthModelConfigService.query(healthModelConfigQueryDto);
    }
}