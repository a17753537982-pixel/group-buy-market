package cn.bugstack.domain.trade.adapter.respository;

import cn.bugstack.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import cn.bugstack.domain.trade.model.entity.MarketPayOrderEntity;
import cn.bugstack.domain.trade.model.valobj.GroupBuyProgressVO;

public interface ITradeRepository {
    public MarketPayOrderEntity queryNoPayOrderEntityByOutTradeNo(String userId, String outTradeNo);

    public GroupBuyProgressVO queryGroupBuyProgress(String teamId) ;

    public MarketPayOrderEntity lockMarketPayOrder(GroupBuyOrderAggregate groupBuyOrderAggregate) ;

    //操作数据库是response 操作端口的是port


}
