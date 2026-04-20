package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.GroupBuyOrderList;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IGroupBuyOrderListDao {

    //插入拼团订单详情
    void insert(GroupBuyOrderList groupBuyOrderListReq);

    //根据外部交易交易单号查询订单
    GroupBuyOrderList queryGroupBuyOrderRecordOutTradeNo(GroupBuyOrderList groupBuyOrderListReq);
}
