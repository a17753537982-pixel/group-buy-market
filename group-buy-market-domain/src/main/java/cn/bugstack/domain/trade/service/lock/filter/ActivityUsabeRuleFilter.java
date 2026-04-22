package cn.bugstack.domain.trade.service.lock.filter;

import cn.bugstack.domain.trade.adapter.respository.ITradeRepository;
import cn.bugstack.domain.trade.model.entity.GroupByActivityEntity;
import cn.bugstack.domain.trade.model.entity.TradeRuleCommandEntity;
import cn.bugstack.domain.trade.model.entity.TradeRuleFilterBackEntity;
import cn.bugstack.domain.trade.service.lock.factory.TradeRuleFilterFactory;
import cn.bugstack.types.design.framework.link.model2.handler.ILogicHandler;
import cn.bugstack.types.enums.ActivityStatusEnumVO;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Slf4j
@Service
public class ActivityUsabeRuleFilter implements ILogicHandler <TradeRuleCommandEntity, TradeRuleFilterFactory.DynamicContext, TradeRuleFilterBackEntity>{


    @Resource
    private ITradeRepository repository;

    @Override
    public TradeRuleFilterBackEntity apply(TradeRuleCommandEntity requestParameter, TradeRuleFilterFactory.DynamicContext dynamicContext) throws Exception {

        //查询活动，进行过滤
        log.info("交易队则过滤-用户{}活动有效性校验 cativityId:{}",requestParameter.getUserId(),requestParameter.getActivityId());


        GroupByActivityEntity groupByActivity=repository.queryGroupByActivityByActivityId(requestParameter.getActivityId());

        if(!ActivityStatusEnumVO.EFFECTIVE.equals(groupByActivity.getStatus()))
        {
            throw new AppException(ResponseCode.E0101);
        }
        Date currentTime=new Date();
        if(currentTime.before(groupByActivity.getStartTime()) || currentTime.after(groupByActivity.getEndTime()))
        {
            throw new AppException(ResponseCode.E0102);
        }

        dynamicContext.setGroupByActivity(groupByActivity);

        return next(requestParameter,dynamicContext);
    }
}
