package cn.kmbeast.service.impl;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.mapper.NewsMapper;
import cn.kmbeast.mapper.ScoreMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.PageResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.NewsQueryDto;
import cn.kmbeast.pojo.dto.query.extend.ScoreQueryDto;
import cn.kmbeast.pojo.entity.News;
import cn.kmbeast.pojo.vo.NewsVO;
import cn.kmbeast.pojo.vo.ScoreVO;
import cn.kmbeast.service.NewsService;
import cn.kmbeast.utils.UserBasedCFUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 健康资讯业务逻辑实现
 */
@Service
public class NewsServiceImpl implements NewsService {

    @Resource
    private NewsMapper newsMapper;
    @Resource
    private ScoreMapper scoreMapper;

    /**
     * 健康资讯新增
     *
     * @param news 参数
     * @return Result<Void>
     */
    @Override
    public Result<Void> save(News news) {
        news.setCreateTime(LocalDateTime.now());
        newsMapper.save(news);
        return ApiResult.success();
    }

    /**
     * 健康资讯删除
     *
     * @param ids 参数
     * @return Result<Void>
     */
    @Override
    public Result<Void> batchDelete(List<Long> ids) {
        newsMapper.batchDelete(ids);
        return ApiResult.success();
    }

    /**
     * 健康资讯修改
     *
     * @param news 参数
     * @return Result<Void>
     */
    @Override
    public Result<Void> update(News news) {
        newsMapper.update(news);
        return ApiResult.success();
    }

    /**
     * 健康资讯查询
     *
     * @param newsQueryDto 查询参数
     * @return Result<List < NewsVO>>
     */
    @Override
    public Result<List<NewsVO>> query(NewsQueryDto newsQueryDto) {
        List<NewsVO> newsVOS = newsMapper.query(newsQueryDto);
        Integer totalCount = newsMapper.queryCount(newsQueryDto);
        return PageResult.success(newsVOS, totalCount);
    }

    private static Map<Integer, Map<Integer, Double>> buildUserItemMatrix(List<NewsVO> newsVOS, List<ScoreVO> scoreVOS) {
        // 获取所有新闻ID
        Set<Integer> allNewsIds = new HashSet<>();
        for (NewsVO newsVO : newsVOS) {
            allNewsIds.add(newsVO.getId());
        }

        // 获取所有用户ID
        Set<Integer> allUserIds = new HashSet<>();
        for (ScoreVO scoreVO : scoreVOS) {
            allUserIds.add(scoreVO.getUserId());
        }

        // 构建用户-物品评分矩阵
        Map<Integer, Map<Integer, Double>> userItemMatrix = new HashMap<>();

        // 初始化每个用户的评分映射，默认值为0
        for (Integer userId : allUserIds) {
            Map<Integer, Double> itemScores = new HashMap<>();
            for (Integer newsId : allNewsIds) {
                itemScores.put(newsId, 0.0);
            }
            userItemMatrix.put(userId, itemScores);
        }

        // 填充已有评分
        for (ScoreVO scoreVO : scoreVOS) {
            Integer userId = scoreVO.getUserId();
            Integer newsId = scoreVO.getArticleId();
            Double score = Double.valueOf(scoreVO.getTotal());

            // 更新用户对应的评分映射
            if (userItemMatrix.containsKey(userId)) {
                userItemMatrix.get(userId).put(newsId, score);
            }
        }

        return userItemMatrix;
    }

    /**
     * 生成推荐资讯
     *
     * @param count 生成的数量
     */
    @Override
    public Result<List<NewsVO>> queryRecommend(Integer count) {
        System.out.println("==============协同过滤推荐==============");
        NewsQueryDto newsQueryDto = new NewsQueryDto();
        // 资讯数据源
        List<NewsVO> newsVOS = newsMapper.query(newsQueryDto);
        ScoreQueryDto scoreQueryDto = new ScoreQueryDto();
        // 评分数据源
        List<ScoreVO> scoreVOS = scoreMapper.query(scoreQueryDto);
        // 构建用户-物品评分矩阵
        Map<Integer, Map<Integer, Double>> userItemMatrix = buildUserItemMatrix(newsVOS, scoreVOS);
        // 打印结果 ----- 方便调试
        printUserItemMatrix(userItemMatrix);
        UserBasedCFUtil cfUtil = new UserBasedCFUtil(userItemMatrix);
        List<Integer> recommendedItems = cfUtil.recommendItems(LocalThreadHolder.getUserId(), count);
        System.out.println("推荐列表：" + recommendedItems);
        System.out.println("=====================================");
        if (!recommendedItems.isEmpty()) {
            List<NewsVO> newsVOList = newsMapper.queryIds(recommendedItems);
            return ApiResult.success(newsVOList);
        }
        return ApiResult.success(newsVOS.subList(newsVOS.size() - count, newsVOS.size()));
    }

    // 打印评分矩阵，方便调试
    private static void printUserItemMatrix(Map<Integer, Map<Integer, Double>> userItemMatrix) {

        System.out.println("当前用户评分矩阵：");

        // 获取所有新闻ID并排序
        Set<Integer> allNewsIds = new TreeSet<>();
        for (Map<Integer, Double> itemScores : userItemMatrix.values()) {
            allNewsIds.addAll(itemScores.keySet());
        }

        // 打印表头
        System.out.print("用户ID\t");
        for (Integer newsId : allNewsIds) {
            System.out.print("物品" + newsId + "\t");
        }
        System.out.println();

        // 打印每一行数据
        for (Map.Entry<Integer, Map<Integer, Double>> entry : userItemMatrix.entrySet()) {
            System.out.print(entry.getKey() + "\t");
            for (Integer newsId : allNewsIds) {
                Double score = entry.getValue().getOrDefault(newsId, 0.0);
                System.out.print(score + "\t");
            }
            System.out.println();
        }
    }

}
