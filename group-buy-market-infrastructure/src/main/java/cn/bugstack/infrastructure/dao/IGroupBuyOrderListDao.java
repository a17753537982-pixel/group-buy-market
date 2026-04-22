package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.GroupBuyOrderList;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IGroupBuyOrderListDao {

    //插入拼团订单详情
    void insert(GroupBuyOrderList groupBuyOrderListReq);

    //根据外部交易交易单号查询订单
    GroupBuyOrderList queryGroupBuyOrderRecordOutTradeNo(GroupBuyOrderList groupBuyOrderListReq);


    //查询参与活动的次数
    Integer queryOrderCountByActivityId(GroupBuyOrderList groupBuyOrderListReq);

    //更新订单状态改为2Complete
    Integer updateOrderStatus2COMPLETE(GroupBuyOrderList groupBuyOrderListReq);

    //根据teamId查询TradeNoList，写入回调表里
    List<String> queryGroupBuyCompleteOrderOutTradeNoListByTeamId(String teamId);
}
