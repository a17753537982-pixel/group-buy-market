package cn.bugstack.domain.trade.service;

import cn.bugstack.domain.trade.adapter.respository.ITradeRepository;
import cn.bugstack.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import cn.bugstack.domain.trade.model.entity.*;
import cn.bugstack.domain.trade.model.valobj.GroupBuyProgressVO;
import cn.bugstack.domain.trade.service.factory.TradeRuleFilterFactory;
import cn.bugstack.types.design.framework.link.model2.chain.BusinessLinkedList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class TradeOrderService implements ITradeOrderService{

    @Resource
    private ITradeRepository repository;


    @Resource
    private BusinessLinkedList<TradeRuleCommandEntity, TradeRuleFilterFactory.DynamicContext, TradeRuleFilterBackEntity> tradeOrderFilter;

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

        TradeRuleCommandEntity tradeRuleCommandEntity=new TradeRuleCommandEntity(userEntity.getUserId(), payActivityEntity.getActivityId());
        TradeRuleFilterBackEntity tradeRuleFilterBackEntity = tradeOrderFilter.apply(tradeRuleCommandEntity, new TradeRuleFilterFactory.DynamicContext());



        GroupBuyOrderAggregate groupBuyOrderAggregate = GroupBuyOrderAggregate.builder()
                .userEntity(userEntity)
                .payDiscountEntity(payDiscountEntity)
                .payActivityEntity(payActivityEntity)
                .userTakeOrderCount(tradeRuleFilterBackEntity.getUserTakeOrderCount())
                .build();
        return repository.lockMarketPayOrder(groupBuyOrderAggregate);
    }
}
