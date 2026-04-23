package cn.bugstack.domain.trade.service.lock;

import cn.bugstack.domain.trade.adapter.repository.ITradeRepository;
import cn.bugstack.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import cn.bugstack.domain.trade.model.entity.*;
import cn.bugstack.domain.trade.model.valobj.GroupBuyProgressVO;
import cn.bugstack.domain.trade.service.ITradeLockOrderService;
import cn.bugstack.domain.trade.service.lock.factory.TradeRuleFilterFactory;
import cn.bugstack.types.design.framework.link.model2.chain.BusinessLinkedList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class TradeLockOrderService implements ITradeLockOrderService {

    @Resource
    private ITradeRepository repository;


    @Resource
    private BusinessLinkedList<TradeLockRuleCommandEntity, TradeRuleFilterFactory.DynamicContext, TradeLockRuleFilterBackEntity> tradeOrderFilter;

    @Override
    public MarketPayOrderEntity queryNoPayOrderEntityByOutTradeNo(String userId, String outTradeNo) {
        log.info("拼团交易：查询未支付营销订单{},{}",userId,outTradeNo);
        return repository.queryNoPayOrderEntityByOutTradeNo(userId,outTradeNo);
    }

    @Override
    public GroupBuyProgressVO queryGroupBuyProgress(String teamId) {
        log.info("拼团交易：查询拼团进度{},{}",teamId);
        return repository.queryGroupBuyProgress(teamId);
    }

    @Override
    public MarketPayOrderEntity lockMarketPayOrder(UserEntity userEntity, PayActivityEntity payActivityEntity, PayDiscountEntity payDiscountEntity) throws Exception {

        log.info("拼团交易，锁定营销优惠订单:{},activityId{} goodsId{}",userEntity.getUserId(),payActivityEntity.getActivityId(),payDiscountEntity.getGoodsId());

        TradeLockRuleCommandEntity tradeLockRuleCommandEntity =new TradeLockRuleCommandEntity(userEntity.getUserId(), payActivityEntity.getActivityId());
        TradeLockRuleFilterBackEntity tradeLockRuleFilterBackEntity = tradeOrderFilter.apply(tradeLockRuleCommandEntity, new TradeRuleFilterFactory.DynamicContext());


        GroupBuyOrderAggregate groupBuyOrderAggregate = GroupBuyOrderAggregate.builder()
                .userEntity(userEntity)
                .payDiscountEntity(payDiscountEntity)
                .payActivityEntity(payActivityEntity)
                .userTakeOrderCount(tradeLockRuleFilterBackEntity.getUserTakeOrderCount())
                .build();
        return repository.lockMarketPayOrder(groupBuyOrderAggregate);
    }
}
