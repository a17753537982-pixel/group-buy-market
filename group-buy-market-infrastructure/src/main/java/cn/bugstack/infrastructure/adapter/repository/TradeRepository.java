package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.domain.trade.adapter.respository.ITradeRepository;
import cn.bugstack.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import cn.bugstack.domain.trade.model.entity.MarketPayOrderEntity;
import cn.bugstack.domain.trade.model.entity.PayActivityEntity;
import cn.bugstack.domain.trade.model.entity.PayDiscountEntity;
import cn.bugstack.domain.trade.model.entity.UserEntity;
import cn.bugstack.domain.trade.model.valobj.GroupBuyProgressVO;
import cn.bugstack.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import cn.bugstack.infrastructure.dao.IGroupBuyOrderDao;
import cn.bugstack.infrastructure.dao.IGroupBuyOrderListDao;
import cn.bugstack.infrastructure.dao.po.GroupBuyOrder;
import cn.bugstack.infrastructure.dao.po.GroupBuyOrderList;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Repository
public class TradeRepository implements ITradeRepository {

    @Resource
    private IGroupBuyOrderDao groupBuyOrderDao;

    @Resource
    private IGroupBuyOrderListDao groupBuyOrderListDao;

    @Override
    public MarketPayOrderEntity queryNoPayOrderEntityByOutTradeNo(String userId, String outTradeNo) {
        GroupBuyOrderList groupBuyOrderListReq = GroupBuyOrderList.builder()
                .userId(userId).outTradeNo(outTradeNo).build();
        GroupBuyOrderList groupBuyOrderListRes = groupBuyOrderListDao.queryGroupBuyOrderRecordOutTradeNo(groupBuyOrderListReq);
        if(groupBuyOrderListRes==null) return null;
        return MarketPayOrderEntity
                .builder()
                .orderId(groupBuyOrderListRes.getOrderId())
                .tradeOrderStatusEnumVO(TradeOrderStatusEnumVO.valueOf(groupBuyOrderListRes.getStatus()))
                .build();

    }

    @Override
    public GroupBuyProgressVO queryGroupBuyProgress(String teamId) {
        GroupBuyOrder groupBuyOrderRes = groupBuyOrderDao.queryGroupBuyProgress(teamId);
        if(null==groupBuyOrderRes) return null;
        return GroupBuyProgressVO.builder()
                .targetCount(groupBuyOrderRes.getTargetCount())
                .completeCount(groupBuyOrderRes.getCompleteCount())
                .lockCount(groupBuyOrderRes.getLockCount())
                .build();
    }


    @Transactional(timeout = 500)
    @Override
    public MarketPayOrderEntity lockMarketPayOrder(GroupBuyOrderAggregate groupBuyOrderAggregate) {
        UserEntity userEntity = groupBuyOrderAggregate.getUserEntity();
        PayActivityEntity payActivityEntity = groupBuyOrderAggregate.getPayActivityEntity();
        PayDiscountEntity payDiscountEntity = groupBuyOrderAggregate.getPayDiscountEntity();

        String teamId = payActivityEntity.getTeamId();
        if(StringUtils.isBlank(teamId))
        {
            //插入一条记录
            teamId= RandomStringUtils.randomAlphabetic(8);
            GroupBuyOrder groupBuyOrder = GroupBuyOrder.builder()
                    .activityId(payActivityEntity.getActivityId())
                    .channel(payDiscountEntity.getChannel())
                    .source(payDiscountEntity.getSource())
                    .deductionPrice(payDiscountEntity.getDeductionPrice())
                    .originalPrice(payDiscountEntity.getOriginalPrice())
                    .teamId(teamId)
                    .payPrice(payDiscountEntity.getDeductionPrice()).targetCount(payActivityEntity.getTargetCount()).completeCount(0).lockCount(1).status(0)
                    .build();
            groupBuyOrderDao.insert(groupBuyOrder);
        }
        else
        {
            int res = groupBuyOrderDao.updateAddLockCount(teamId);
            if(1!=res)
            {
                throw new AppException(ResponseCode.E0005.getCode(),ResponseCode.E0005.getInfo());
            }

        }

        String orderId=RandomStringUtils.randomAlphabetic(12);

        GroupBuyOrderList groupBuyOrderList = GroupBuyOrderList
                .builder()
                .userId(userEntity.getUserId())
                .teamId(teamId)
                .orderId(orderId)
                .activityId(payActivityEntity.getActivityId())
                .startTime(payActivityEntity.getStartTime())
                .endTime(payActivityEntity.getEndTime())
                .goodsId(payDiscountEntity.getGoodsId())
                .source(payDiscountEntity.getSource()).channel(payDiscountEntity.getChannel())
                .originalPrice(payDiscountEntity.getOriginalPrice()).deductionPrice(payDiscountEntity.getDeductionPrice())
                .status(TradeOrderStatusEnumVO.CREATE.getCode())
                .outTradeNo(payDiscountEntity.getOutTradeNo()).build();
        try {
            groupBuyOrderListDao.insert(groupBuyOrderList);
        }catch (DuplicateKeyException e)
        {
            throw new AppException(ResponseCode.INDEX_EXCEPTION.getCode(),ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        return MarketPayOrderEntity.builder().orderId(orderId).deductionPrice(payDiscountEntity.getDeductionPrice()).tradeOrderStatusEnumVO(TradeOrderStatusEnumVO.CREATE)
                .build();
    }
}
