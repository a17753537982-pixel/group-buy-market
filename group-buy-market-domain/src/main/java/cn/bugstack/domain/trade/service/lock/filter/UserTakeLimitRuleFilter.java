package cn.bugstack.domain.trade.service.lock.filter;

import cn.bugstack.domain.trade.adapter.respository.ITradeRepository;
import cn.bugstack.domain.trade.model.entity.TradeRuleCommandEntity;
import cn.bugstack.domain.trade.model.entity.TradeRuleFilterBackEntity;
import cn.bugstack.domain.trade.service.lock.factory.TradeRuleFilterFactory;
import cn.bugstack.types.design.framework.link.model2.handler.ILogicHandler;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class UserTakeLimitRuleFilter implements ILogicHandler<TradeRuleCommandEntity, TradeRuleFilterFactory.DynamicContext,TradeRuleFilterBackEntity> {


    @Resource
    private ITradeRepository repository;


    @Override
    public TradeRuleFilterBackEntity apply(TradeRuleCommandEntity requestParameter, TradeRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("交易队则过滤-用户{}参与次数校验 activityId:{}",requestParameter.getUserId(),requestParameter.getActivityId());
        Integer count=repository.queryOrderCountByActivityId(requestParameter.getActivityId(),requestParameter.getUserId());
        Integer takeLimitCount = dynamicContext.getGroupByActivity().getTakeLimitCount();
        if(null !=takeLimitCount && count>=takeLimitCount)
        {
            throw new AppException(ResponseCode.E0103);
        }
        return TradeRuleFilterBackEntity.builder().userTakeOrderCount(count).build();
    }
}
