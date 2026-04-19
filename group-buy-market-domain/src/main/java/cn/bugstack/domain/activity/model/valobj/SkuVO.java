package cn.bugstack.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SkuVO {
    private String goodsId;
    /** 商品名称 */
    private String goodsName;
    /** 原始价格 */
    private BigDecimal originalPrice;
}
