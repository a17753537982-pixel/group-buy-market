package cn.bugstack.infrastructure.dao.po;

import lombok.Data;
import java.util.Date;

/**
 * 对应数据库 group_buy_activity 表的 持久化对象 (PO)
 */
@Data
public class GroupBuyActivity {
    private Long id;
    private Long activityId;
    private String source;
    private String channel;
    private String goodsId;
    private String discountId;
    private Integer groupType;
    private Integer takeLimitCount;
    private Integer target;
    private Integer validTime;
    private Integer status;
    private Date startTime;
    private Date endTime;
    private String tagId;
    private String tagScope;
    private Date createTime;
    private Date updateTime;
}