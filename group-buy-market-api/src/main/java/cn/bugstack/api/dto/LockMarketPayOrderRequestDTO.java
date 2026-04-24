package cn.bugstack.api.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LockMarketPayOrderRequestDTO {
    //用户id
    private String userId;
    private String teamId;
    private String source;
    private String channel;
    private Long activityId;
    private String goodsId;
    private String outTradeNo;
    private String notifyUrl;
}
