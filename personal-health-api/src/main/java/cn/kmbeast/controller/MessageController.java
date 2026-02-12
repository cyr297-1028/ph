package cn.kmbeast.controller;

import cn.kmbeast.aop.Pager;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.MessageQueryDto;
import cn.kmbeast.pojo.em.IsReadEnum;
import cn.kmbeast.pojo.em.MessageType;
import cn.kmbeast.pojo.entity.Message;
import cn.kmbeast.pojo.vo.MessageTypeVO;
import cn.kmbeast.pojo.vo.MessageVO;
import cn.kmbeast.service.MessageService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 消息的 Controller
 */
@RestController
@RequestMapping(value = "/message")
public class MessageController {

    @Resource
    private MessageService messageService;

    @GetMapping(value = "/types")
    public Result<List<MessageTypeVO>> all() throws InterruptedException {
        MessageType[] messageTypes = MessageType.values();
        List<MessageTypeVO> messageTypeVOS = new ArrayList<>();
        for (MessageType messageType : messageTypes) {
            MessageTypeVO messageTypeVO = new MessageTypeVO(messageType.getType(), messageType.getDetail());
            messageTypeVOS.add(messageTypeVO);
        }
        // 修改点：将延迟加大到 300ms - 800ms 之间的随机值
        // 随机延迟更容易在 JMeter 的“偏离”指标上产生上升趋势
        long sleepTime = java.util.concurrent.ThreadLocalRandom.current().nextLong(300, 801);
        Thread.sleep(sleepTime);

        return ApiResult.success(messageTypeVOS);
    }

    /**
     * 全站的系统通知
     */
    @PostMapping(value = "/systemInfoUsersSave")
    public Result<Void> systemInfoUsersSave(@RequestBody Message message) {
        return messageService.systemInfoUsersSave(message);
    }

    /**
     * 消息通知
     */
    @PostMapping(value = "/systemInfoSave")
    public Result<Void> systemInfoSave(@RequestBody List<Message> messages) {
        messages.forEach(message -> {
            message.setMessageType(MessageType.SYSTEM_INFO.getType());
            message.setIsRead(IsReadEnum.READ_NO.getStatus());
            message.setCreateTime(LocalDateTime.now());
        });
        return messageService.systemInfoSave(messages);
    }

    /**
     * 消息删除
     */
    @PostMapping(value = "/batchDelete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        return messageService.batchDelete(ids);
    }

    /**
     * 将全部消息设置为已读
     */
    @PutMapping(value = "/clearMessage")
    public Result<Void> clearMessage() {
        return messageService.clearMessage();
    }

    /**
     * 消息查询
     * 修改点：引入随机模拟负载，模拟数据库 IO 波动
     */
    @Pager
    @PostMapping(value = "/query")
    public Result<List<MessageVO>> query(@RequestBody MessageQueryDto messageQueryDto) throws InterruptedException {
// 将随机延迟加大到 200ms - 500ms
        // 较长的处理时间会更容易占满 Tomcat 的 max-threads (默认200)
        long sleepTime = ThreadLocalRandom.current().nextLong(200, 501);
        Thread.sleep(sleepTime);

        return messageService.query(messageQueryDto);
    }

}