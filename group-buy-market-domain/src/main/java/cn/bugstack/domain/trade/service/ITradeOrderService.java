package cn.bugstack.domain.trade.service;

import cn.bugstack.domain.trade.model.entity.MarketPayOrderEntity;
import cn.bugstack.domain.trade.model.entity.PayActivityEntity;
import cn.bugstack.domain.trade.model.entity.PayDiscountEntity;
import cn.bugstack.domain.trade.model.entity.UserEntity;
import cn.bugstack.domain.trade.model.valobj.GroupBuyProgressVO;

public interface ITradeOrderService {

    //查询未支付营销订单
    MarketPayOrderEntity queryNoPayOrderEntityByOutTradeNo(String userId, String outTradeNo);

    //查询拼单进度
    public GroupBuyProgressVO queryGroupBuyProgress(String teamId);

    //锁定营销优惠支付订单----用户实体---支付活动实体----折扣实体
    public MarketPayOrderEntity lockMarketPayOrder(UserEntity userEntity, PayActivityEntity payActivityEntity, PayDiscountEntity payDiscountEntity);

}
