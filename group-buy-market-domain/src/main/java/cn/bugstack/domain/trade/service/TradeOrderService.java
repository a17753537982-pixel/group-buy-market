package cn.bugstack.domain.trade.service;

import cn.bugstack.domain.trade.adapter.respository.ITradeRepository;
import cn.bugstack.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import cn.bugstack.domain.trade.model.entity.MarketPayOrderEntity;
import cn.bugstack.domain.trade.model.entity.PayActivityEntity;
import cn.bugstack.domain.trade.model.entity.PayDiscountEntity;
import cn.bugstack.domain.trade.model.entity.UserEntity;
import cn.bugstack.domain.trade.model.valobj.GroupBuyProgressVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class TradeOrderService implements ITradeOrderService{

    @Resource
    private ITradeRepository repository;

    @Override
    public MarketPayOrderEntity queryNoPayOrderEntityByOutTradeNo(String userId, String outTradeNo) {
        return repository.queryNoPayOrderEntityByOutTradeNo(userId,outTradeNo);
    }

    @Override
    public GroupBuyProgressVO queryGroupBuyProgress(String teamId) {
        return repository.queryGroupBuyProgress(teamId);
    }

    @Override
    public MarketPayOrderEntity lockMarketPayOrder(UserEntity userEntity, PayActivityEntity payActivityEntity, PayDiscountEntity payDiscountEntity) {
        GroupBuyOrderAggregate groupBuyOrderAggregate = GroupBuyOrderAggregate.builder()
                .userEntity(userEntity)
                .payDiscountEntity(payDiscountEntity)
                .payActivityEntity(payActivityEntity)
                .build();
        return repository.lockMarketPayOrder(groupBuyOrderAggregate);
    }
}
