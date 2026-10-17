package com.ruoyi.wash.worker.listener;

import com.ruoyi.wash.order.event.OrderKeyInEvent;
import com.ruoyi.wash.worker.service.WashDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 钥匙入柜 → 自动派单。
 *
 * <p>wash-order 只发事件不依赖 wash-worker（依赖方向单向），派单规则全在 wash-worker。
 *
 * <p>同步执行（项目未开 @EnableAsync，加 @Async 也是空转）：派单只是几次查询 + 一次更新，
 * 同步还能保证柜机回调返回时任务已经落位，师傅端立刻可见。派单失败绝不影响钥匙入柜本身，
 * 所以这里 catch 全部异常只记日志。
 */
@Component
public class WorkerDispatchListener {

    private static final Logger log = LoggerFactory.getLogger(WorkerDispatchListener.class);

    @Autowired
    private WashDispatchService dispatchService;

    @EventListener
    public void onKeyIn(OrderKeyInEvent event) {
        try {
            dispatchService.dispatchOnKeyIn(event.orderNo(), event.communityId(), event.siteId());
        } catch (Exception e) {
            log.error("[派单] 自动派单异常（不影响钥匙入柜）orderNo={}", event.orderNo(), e);
        }
    }
}
