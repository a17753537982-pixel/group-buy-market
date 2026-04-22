package cn.bugstack.domain.trade.service.settlement;

import cn.bugstack.domain.trade.adapter.respository.ITradeRepository;
import cn.bugstack.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import cn.bugstack.domain.trade.model.entity.*;
import cn.bugstack.domain.trade.service.ITradeSettlementOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class TradeSettlementOrderService implements ITradeSettlementOrderService {

    @Resource
    private ITradeRepository repository;

    @Override
    public TradePaySettlementEntity settlementMarketPayOrder(TradePaySuccessEntity tradePaySuccuessEntity) {
        log.info("拼团交易-支付订单结算：{},outTradeNo{}",tradePaySuccuessEntity.getUserId(),tradePaySuccuessEntity.getOutTradeNo());
        //1.查询拼团信息
        MarketPayOrderEntity marketPayOrderEntity = repository.queryNoPayOrderEntityByOutTradeNo(tradePaySuccuessEntity.getUserId(), tradePaySuccuessEntity.getOutTradeNo());
        if(null==marketPayOrderEntity)
        {
            log.info("不存在的外部交易单号，或者用户已经退单，不需要做订单结算{} outTradeNO{}",tradePaySuccuessEntity.getUserId(),tradePaySuccuessEntity.getOutTradeNo());
            return null;
        }

        //2.查询组团信息
        GroupBuyTeamEntity groupBuyTeamEntity=repository.queryGroupBuyTeamByTeamId(marketPayOrderEntity.getTeamId());

        //3.构建聚合对象
        GroupBuyTeamSettlementAggregate groupBuyTeamSettlementAggregate = GroupBuyTeamSettlementAggregate.builder()
                .userEntity(UserEntity.builder().userId(tradePaySuccuessEntity.getUserId()).build())
                .groupBuyTeamEntity(groupBuyTeamEntity)
                .tradePaySuccessEntity(tradePaySuccuessEntity)
                .build();

        repository.settlementMarketPayOrder(groupBuyTeamSettlementAggregate);

        return TradePaySettlementEntity.builder().source(tradePaySuccuessEntity.getSource()).channel(tradePaySuccuessEntity.getChannel())
                .userId(tradePaySuccuessEntity.getUserId()).teamId(marketPayOrderEntity.getTeamId()).activityId(groupBuyTeamEntity.getActivityId())
                .outTradeNo(tradePaySuccuessEntity.getOutTradeNo()).build();
    }
}
