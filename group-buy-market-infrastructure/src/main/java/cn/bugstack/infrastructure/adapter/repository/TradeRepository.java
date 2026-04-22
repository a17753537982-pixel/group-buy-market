package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.domain.trade.adapter.respository.ITradeRepository;
import cn.bugstack.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import cn.bugstack.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import cn.bugstack.domain.trade.model.entity.*;
import cn.bugstack.domain.trade.model.valobj.GroupBuyProgressVO;
import cn.bugstack.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import cn.bugstack.infrastructure.dao.IGroupBuyActivityDao;
import cn.bugstack.infrastructure.dao.IGroupBuyOrderDao;
import cn.bugstack.infrastructure.dao.IGroupBuyOrderListDao;
import cn.bugstack.infrastructure.dao.INotifyTaskDao;
import cn.bugstack.infrastructure.dao.po.GroupBuyActivity;
import cn.bugstack.infrastructure.dao.po.GroupBuyOrder;
import cn.bugstack.infrastructure.dao.po.GroupBuyOrderList;
import cn.bugstack.infrastructure.dao.po.NotifyTask;
import cn.bugstack.types.common.Constants;
import cn.bugstack.types.enums.ActivityStatusEnumVO;
import cn.bugstack.types.enums.GroupBuyOrderEnumVO;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;

@Repository
public class TradeRepository implements ITradeRepository {

    @Resource
    private IGroupBuyOrderDao groupBuyOrderDao;

    @Resource
    private IGroupBuyOrderListDao groupBuyOrderListDao;

    @Resource
    private IGroupBuyActivityDao groupBuyActivityDao;

    @Resource
    private INotifyTaskDao notifyTaskDao;

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
                .teamId(groupBuyOrderListRes.getTeamId())
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


    @Transactional(timeout = 500)//注意涉及两个数据库操作 要开启事务
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
                    .payPrice(payDiscountEntity.getPayPrice()).targetCount(payActivityEntity.getTargetCount()).completeCount(0).lockCount(1).status(0)
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
        Integer count = groupBuyOrderAggregate.getUserTakeOrderCount();

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
                .bizId(payActivityEntity.getActivityId()+ Constants.UNDERLINE +userEntity.getUserId()+Constants.UNDERLINE+(count+1))
                .status(TradeOrderStatusEnumVO.CREATE.getCode())
                .outTradeNo(payDiscountEntity.getOutTradeNo()).build();
        try {
            groupBuyOrderListDao.insert(groupBuyOrderList);
        }catch (DuplicateKeyException e)
        {
            throw new AppException(ResponseCode.INDEX_EXCEPTION.getCode(),ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        return MarketPayOrderEntity.builder().teamId(teamId).orderId(orderId).deductionPrice(payDiscountEntity.getDeductionPrice()).tradeOrderStatusEnumVO(TradeOrderStatusEnumVO.CREATE)
                .build();
    }

    @Override
    public Integer queryOrderCountByActivityId(Long activityId, String userId) {

        GroupBuyOrderList groupBuyOrderListReq=new GroupBuyOrderList();
        groupBuyOrderListReq.setActivityId(activityId);
        groupBuyOrderListReq.setUserId(userId);
        return groupBuyOrderListDao.queryOrderCountByActivityId(groupBuyOrderListReq);
    }

    @Override
    public GroupByActivityEntity queryGroupByActivityByActivityId(Long activityId) {
        GroupBuyActivity groupByActivity=groupBuyActivityDao.queryGroupBuyActivityByActivityId( activityId);
        return GroupByActivityEntity.builder()
                .activityId(groupByActivity.getActivityId())
                .activityName(groupByActivity.getActivityName())
                .discountId(groupByActivity.getDiscountId())
                .groupType(groupByActivity.getGroupType())
                .takeLimitCount(groupByActivity.getTakeLimitCount())
                .target(groupByActivity.getTarget())
                .validTime(groupByActivity.getValidTime())
                .status(ActivityStatusEnumVO.valueOf(groupByActivity.getStatus()))
                .startTime(groupByActivity.getStartTime())
                .endTime(groupByActivity.getEndTime())
                .tagId(groupByActivity.getTagId())
                .tagScope(groupByActivity.getTagScope())
                .build();
    }

    @Override
    public GroupBuyTeamEntity queryGroupBuyTeamByTeamId(String teamId) {
        GroupBuyOrder groupBuyOrderRes = groupBuyOrderDao.queryGroupBuyTeamByTeamId(teamId);

        GroupBuyTeamEntity groupBuyTeamEntity = GroupBuyTeamEntity.builder()
                .teamId(groupBuyOrderRes.getTeamId())
                .activityId(groupBuyOrderRes.getActivityId())
                .targetCount(groupBuyOrderRes.getTargetCount())
                .completeCount(groupBuyOrderRes.getCompleteCount())
                .lockCount(groupBuyOrderRes.getLockCount())
                .status(GroupBuyOrderEnumVO.valueOf(groupBuyOrderRes.getStatus()))
                .build();
        return groupBuyTeamEntity;
    }

    @Override
    @Transactional(timeout = 500)
    public void settlementMarketPayOrder(GroupBuyTeamSettlementAggregate groupBuyTeamSettlementAggregate) {
        UserEntity userEntity = groupBuyTeamSettlementAggregate.getUserEntity();
        GroupBuyTeamEntity groupBuyTeamEntity = groupBuyTeamSettlementAggregate.getGroupBuyTeamEntity();
        TradePaySuccessEntity tradePaySuccessEntity = groupBuyTeamSettlementAggregate.getTradePaySuccessEntity();

        //更新订单明细表的状态
        GroupBuyOrderList groupBuyOrderListReq=new GroupBuyOrderList();
        groupBuyOrderListReq.setUserId(userEntity.getUserId());
        groupBuyOrderListReq.setOutTradeNo(tradePaySuccessEntity.getOutTradeNo());
        Integer res = groupBuyOrderListDao.updateOrderStatus2COMPLETE(groupBuyOrderListReq);
        if(1!=res)
        {
            throw new AppException(ResponseCode.E0005);
        }

        //更新拼团订单达成数量
        int updateAddCount = groupBuyOrderDao.updateAddCompleteCount(groupBuyTeamEntity.getTeamId());
        if(1!=updateAddCount)
        {
            throw new AppException(ResponseCode.E0005);
        }

        //更新拼团完成状态
        if(groupBuyTeamEntity.getTargetCount()- groupBuyTeamEntity.getCompleteCount()==1) {
            int updateOrderStatus = groupBuyOrderDao.updateOrderStatus2COMPLETE(groupBuyTeamEntity.getTeamId());
            if (1 != updateOrderStatus) {
                throw new AppException(ResponseCode.E0005);
            }

            //写入回调表 接下来发货!
            //查询交易完成外部单号列表：
            List<String> outTradeNoList = groupBuyOrderListDao.queryGroupBuyCompleteOrderOutTradeNoListByTeamId(groupBuyTeamEntity.getTeamId());

            NotifyTask notifyTask = new NotifyTask();
            notifyTask.setActivityId(groupBuyTeamEntity.getActivityId());
            notifyTask.setTeamId(groupBuyTeamEntity.getTeamId());
            notifyTask.setNotifyUrl("暂无");
            notifyTask.setNotifyCount(0);
            notifyTask.setNotifyStatus(0);
            HashMap<String, Object> stringObjectHashMap = new HashMap<>();
            stringObjectHashMap.put("teamId", groupBuyTeamEntity.getTeamId());
            stringObjectHashMap.put("outTradeNoList", outTradeNoList);
            notifyTask.setParameterJson(JSON.toJSONString(stringObjectHashMap));
            notifyTaskDao.insert(notifyTask);
        }
    }
}
