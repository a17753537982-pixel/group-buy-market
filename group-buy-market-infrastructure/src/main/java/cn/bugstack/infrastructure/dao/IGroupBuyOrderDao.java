package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.GroupBuyOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IGroupBuyOrderDao {

    //有哪些操作？
    void insert(GroupBuyOrder groupBuyOrder);

    //更新锁单数量
    int updateAddLockCount(String teamId);

    //减少更新锁单数量
    int updateSubstractionLockCount(String teamId);

    //查询拼团进度
    GroupBuyOrder queryGroupBuyProgress(String teamId);
}
