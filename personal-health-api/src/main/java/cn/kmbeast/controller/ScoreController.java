package cn.kmbeast.controller;

import cn.kmbeast.aop.Pager;
import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.ScoreQueryDto;
import cn.kmbeast.pojo.entity.Score;
import cn.kmbeast.pojo.vo.ScoreVO;
import cn.kmbeast.service.ScoreService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 评分的 Controller
 */
@RestController
@RequestMapping(value = "/score")
public class ScoreController {

    @Resource
    private ScoreService scoreService;

    /**
     * 评分新增
     *
     * @param score 新增数据
     * @return Result<Void> 通用响应体
     */
    @PostMapping(value = "/save")
    public Result<Void> save(@RequestBody Score score) {
        return scoreService.save(score);
    }

    /**
     * 评分查询
     *
     * @param scoreQueryDto 查询参数
     * @return Result<List < ScoreVO>> 通用响应
     */
    @Pager
    @PostMapping(value = "/query")
    public Result<List<ScoreVO>> query(@RequestBody ScoreQueryDto scoreQueryDto) {

        scoreQueryDto.setUserId(LocalThreadHolder.getUserId());

        return scoreService.query(scoreQueryDto);
    }

}
