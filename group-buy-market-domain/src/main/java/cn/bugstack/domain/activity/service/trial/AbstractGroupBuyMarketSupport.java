package cn.bugstack.domain.activity.service.trial;


import cn.bugstack.domain.activity.adapter.repository.IActivityRepository;
import cn.bugstack.domain.activity.model.entity.MarketProductEntity;
import cn.bugstack.domain.activity.model.entity.TrialBalanceEntity;
import cn.bugstack.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;

import cn.bugstack.types.design.framework.tree.AbstractMultiThreadStateRouter;
import cn.bugstack.types.design.framework.tree.AbstractStrategyRouter;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 抽象的拼团营销支撑类
 * @create 2024-12-14 13:42
 */
public abstract class AbstractGroupBuyMarketSupport<MarketProductEntity, DynamicContext, TrialBalanceEntity> extends AbstractMultiThreadStateRouter<MarketProductEntity, DynamicContext, TrialBalanceEntity> {

    @Resource
    protected IActivityRepository repository;

    protected long timeout=500;

    @Override
    protected void multiThread(MarketProductEntity requestParameter, DynamicContext dynamicContext) {
        //缺省 抽象类先实现一下 Node不需要就不重写了
    }


}
