package com.water.flowable.delegate;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * @Author: xu.dm
 * @Date: 2020/9/27 14:28
 * @Version: 1.0
 * @Description: TODO
 **/
@Slf4j
public class ReviewNoApprove implements JavaDelegate {
    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("拒绝，userId是：{}",delegateExecution.getVariable("userId"));
    }
}
