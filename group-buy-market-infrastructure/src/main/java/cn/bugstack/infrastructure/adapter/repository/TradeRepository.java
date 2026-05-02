package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.domain.trade.adapter.repository.ITradeRepository;
import cn.bugstack.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import cn.bugstack.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import cn.bugstack.domain.trade.model.entity.*;
import cn.bugstack.domain.trade.model.valobj.GroupBuyProgressVO;
import cn.bugstack.domain.trade.model.valobj.NotifyConfigVO;
import cn.bugstack.domain.trade.model.valobj.NotifyTypeEnumVO;
import cn.bugstack.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import cn.bugstack.infrastructure.dao.IGroupBuyActivityDao;
import cn.bugstack.infrastructure.dao.IGroupBuyOrderDao;
import cn.bugstack.infrastructure.dao.IGroupBuyOrderListDao;
import cn.bugstack.infrastructure.dao.INotifyTaskDao;
import cn.bugstack.infrastructure.dao.po.GroupBuyActivity;
import cn.bugstack.infrastructure.dao.po.GroupBuyOrder;
import cn.bugstack.infrastructure.dao.po.GroupBuyOrderList;
import cn.bugstack.infrastructure.dao.po.NotifyTask;
import cn.bugstack.infrastructure.dcc.DCCService;
import cn.bugstack.infrastructure.redis.IRedisService;
import cn.bugstack.types.common.Constants;
import cn.bugstack.types.enums.ActivityStatusEnumVO;
import cn.bugstack.types.enums.GroupBuyOrderEnumVO;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 交易仓储服务
 * @create 2025-01-11 09:17
 */
@Slf4j
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

    @Resource
    private DCCService dccService;

    @Value("${spring.rabbitmq.config.producer.topic_team_success.routing_key}")
    private String topic_team_success;

    @Resource
    private IRedisService redisService;

    @Override
    public MarketPayOrderEntity queryNoPayOrderEntityByOutTradeNo(String userId, String outTradeNo) {
        GroupBuyOrderList groupBuyOrderListReq = GroupBuyOrderList.builder()
                .userId(userId).outTradeNo(outTradeNo).build();
        GroupBuyOrderList groupBuyOrderListRes = groupBuyOrderListDao.queryGroupBuyOrderRecordOutTradeNo(groupBuyOrderListReq);
        if(groupBuyOrderListRes==null) return null;
        return MarketPayOrderEntity
                .builder()
                .orderId(groupBuyOrderListRes.getOrderId())
                .originalPrice(groupBuyOrderListRes.getOriginalPrice())
                .deductionPrice(groupBuyOrderListRes.getDeductionPrice())
                .payPrice(groupBuyOrderListRes.getPayPrice())
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
        NotifyConfigVO notifyConfigVO = payDiscountEntity.getNotifyConfigVO();

        String teamId = payActivityEntity.getTeamId();
        if(StringUtils.isBlank(teamId))
        {
            //插入一条记录
            teamId= RandomStringUtils.randomAlphabetic(8);

            // 日期处理
            Date currentDate = new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(currentDate);
            calendar.add(Calendar.MINUTE, payActivityEntity.getValidTime());

            GroupBuyOrder groupBuyOrder = GroupBuyOrder.builder()
                    .activityId(payActivityEntity.getActivityId())
                    .channel(payDiscountEntity.getChannel())
                    .source(payDiscountEntity.getSource())
                    .deductionPrice(payDiscountEntity.getDeductionPrice())
                    .originalPrice(payDiscountEntity.getOriginalPrice())
                    .teamId(teamId)
                    .validStartTime(currentDate)
                    .validEndTime(calendar.getTime())
                    .notifyType(notifyConfigVO.getNotifyType().getCode())
                    .notifyUrl(notifyConfigVO.getNotifyUrl())
                    .payPrice(payDiscountEntity.getPayPrice()).targetCount(payActivityEntity.getTargetCount()).completeCount(0).lockCount(1).status(0)
                    .build();
            //写入记录
            groupBuyOrderDao.insert(groupBuyOrder);
        }
        else
        {
            //更新记录-如果更新记录不等于1，则表示拼团已满，抛出异常！
            int res = groupBuyOrderDao.updateAddLockCount(teamId);
            if(1!=res)
            {
                throw new AppException(ResponseCode.E0005);
            }

        }

        //// 使用 RandomStringUtils.randomNumeric 替代公司里使用的雪花算法UUID
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
                .payPrice(payDiscountEntity.getPayPrice())

                //构建bizId 唯一值
                .bizId(payActivityEntity.getActivityId()+ Constants.UNDERLINE +userEntity.getUserId()+Constants.UNDERLINE+(count+1))
                .status(TradeOrderStatusEnumVO.CREATE.getCode())
                .outTradeNo(payDiscountEntity.getOutTradeNo()).build();
        try {
            //写入拼团记录
            groupBuyOrderListDao.insert(groupBuyOrderList);
        }catch (DuplicateKeyException e)
        {
            throw new AppException(ResponseCode.INDEX_EXCEPTION.getCode(),ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        return MarketPayOrderEntity.builder().teamId(teamId).orderId(orderId)
                .deductionPrice(payDiscountEntity.getDeductionPrice())
                .tradeOrderStatusEnumVO(TradeOrderStatusEnumVO.CREATE)
                .originalPrice(payDiscountEntity.getOriginalPrice())
                .payPrice(payDiscountEntity.getPayPrice())
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
    public GroupBuyActivityEntity queryGroupByActivityByActivityId(Long activityId) {
        GroupBuyActivity groupByActivity=groupBuyActivityDao.queryGroupBuyActivityByActivityId( activityId);
        return GroupBuyActivityEntity.builder()
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

        return GroupBuyTeamEntity.builder()
                .teamId(groupBuyOrderRes.getTeamId())
                .activityId(groupBuyOrderRes.getActivityId())
                .targetCount(groupBuyOrderRes.getTargetCount())
                .completeCount(groupBuyOrderRes.getCompleteCount())
                .lockCount(groupBuyOrderRes.getLockCount())
                .validStartTime(groupBuyOrderRes.getValidStartTime())
                .validEndTime(groupBuyOrderRes.getValidEndTime())
                .status(GroupBuyOrderEnumVO.valueOf(groupBuyOrderRes.getStatus()))
                .notifyConfigVO(
                        NotifyConfigVO.builder()
                                .notifyType(NotifyTypeEnumVO.valueOf(groupBuyOrderRes.getNotifyType()))
                                .notifyMQ(topic_team_success)
                                .notifyUrl(groupBuyOrderRes.getNotifyUrl())
                                .build()
                )
                .build();
    }

    @Override
    @Transactional(timeout = 500)
    public NotifyTaskEntity settlementMarketPayOrder(GroupBuyTeamSettlementAggregate groupBuyTeamSettlementAggregate) {
        UserEntity userEntity = groupBuyTeamSettlementAggregate.getUserEntity();
        GroupBuyTeamEntity groupBuyTeamEntity = groupBuyTeamSettlementAggregate.getGroupBuyTeamEntity();
        NotifyConfigVO notifyConfigVO = groupBuyTeamEntity.getNotifyConfigVO();
        TradePaySuccessEntity tradePaySuccessEntity = groupBuyTeamSettlementAggregate.getTradePaySuccessEntity();

        //更新订单明细表的状态
        GroupBuyOrderList groupBuyOrderListReq=new GroupBuyOrderList();
        groupBuyOrderListReq.setUserId(userEntity.getUserId());
        groupBuyOrderListReq.setOutTradeNo(tradePaySuccessEntity.getOutTradeNo());
        groupBuyOrderListReq.setOutTradeTime(tradePaySuccessEntity.getOutTradeTime());
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

            notifyTask.setNotifyType(notifyConfigVO.getNotifyType().getCode());
            notifyTask.setNotifyMQ(NotifyTypeEnumVO.MQ.equals(notifyConfigVO.getNotifyType()) ? notifyConfigVO.getNotifyMQ() : null);
            notifyTask.setNotifyUrl(NotifyTypeEnumVO.HTTP.equals(notifyConfigVO.getNotifyType()) ? notifyConfigVO.getNotifyUrl() : null);

            notifyTask.setNotifyCount(0);
            notifyTask.setNotifyStatus(0);

            HashMap<String, Object> stringObjectHashMap = new HashMap<>();
            stringObjectHashMap.put("teamId", groupBuyTeamEntity.getTeamId());
            stringObjectHashMap.put("outTradeNoList", outTradeNoList);

            notifyTask.setParameterJson(JSON.toJSONString(stringObjectHashMap));

            notifyTaskDao.insert(notifyTask);

            return NotifyTaskEntity.builder()
                    .teamId(notifyTask.getTeamId())
                    .notifyType(notifyTask.getNotifyType())
                    .notifyMQ(notifyTask.getNotifyMQ())
                    .notifyUrl(notifyTask.getNotifyUrl())
                    .notifyCount(notifyTask.getNotifyCount())
                    .parameterJson(notifyTask.getParameterJson())
                    .build();
        }
        return null;
    }

    @Override
    public boolean isSCBlackIntercept(String source, String channel) {
        return dccService.isSCBlackIntercept(source, channel);
    }

    @Override
    public List<NotifyTaskEntity> queryUnExecutedNotifyTaskList() {
        List<NotifyTask> notifyTaskList = notifyTaskDao.queryUnExecutedNotifyTaskList();
        if (notifyTaskList.isEmpty()) return new ArrayList<>();

        List<NotifyTaskEntity> notifyTaskEntities = new ArrayList<>();
        for (NotifyTask notifyTask : notifyTaskList) {

            NotifyTaskEntity notifyTaskEntity = NotifyTaskEntity.builder()
                        .teamId(notifyTask.getTeamId())
                        .notifyUrl(notifyTask.getNotifyUrl())
                        .notifyCount(notifyTask.getNotifyCount())
                        .parameterJson(notifyTask.getParameterJson())
                        .build();

            notifyTaskEntities.add(notifyTaskEntity);
        }

        return notifyTaskEntities;
    }

    @Override
    public List<NotifyTaskEntity> queryUnExecutedNotifyTaskList(String teamId) {
        NotifyTask notifyTask = notifyTaskDao.queryUnExecutedNotifyTaskByTeamId(teamId);
        if (null == notifyTask) return new ArrayList<>();
        return Collections.singletonList(NotifyTaskEntity.builder()
                .teamId(notifyTask.getTeamId())
                .notifyUrl(notifyTask.getNotifyUrl())
                .notifyCount(notifyTask.getNotifyCount())
                .parameterJson(notifyTask.getParameterJson())
                .build());
    }

    @Override
    public int updateNotifyTaskStatusSuccess(String teamId) {
        return notifyTaskDao.updateNotifyTaskStatusSuccess(teamId);
    }

    @Override
    public int updateNotifyTaskStatusError(String teamId) {
        return notifyTaskDao.updateNotifyTaskStatusError(teamId);
    }

    @Override
    public int updateNotifyTaskStatusRetry(String teamId) {
        return notifyTaskDao.updateNotifyTaskStatusRetry(teamId);
    }

    @Override
    public boolean occupyTeamStock(String teamStockKey, String recoveryTeamStockKey, Integer target, Integer validTime) {

        //target + recoveryCount 才是当前系统真正允许进入的总人次上限。
        Long recoveryCount = redisService.getAtomicLong(recoveryTeamStockKey);
        recoveryCount= null==recoveryCount? 0:recoveryCount;

        long occupy=redisService.incr(teamStockKey)+1;
        if(occupy> target+recoveryCount)
        {
            redisService.setAtomicLong(teamStockKey,target);
            return false;
        }
        String lockKey=teamStockKey + Constants.UNDERLINE + occupy;
        Boolean status = redisService.setNx(lockKey, validTime + 60, TimeUnit.MINUTES);
        if(!status)
        {
            throw new AppException(ResponseCode.E0008);
        }

        return false;
    }

    @Override
    public void recoveryTeamStock(String recoveryTeamStockKey, Integer validTime) {
        // 首次组队拼团，是没有 teamId 的，所以不需要这个做处理。
        if (StringUtils.isBlank(recoveryTeamStockKey)) return;

        redisService.incr(recoveryTeamStockKey);
    }
}
