package cn.bugstack.domain.activity.service.discount;

import cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum;
import cn.bugstack.domain.activity.model.valobj.GroupBuyActivityDiscountVO;

import java.math.BigDecimal;

public abstract class AbstractDiscountCalculateService implements IDiscountCalculateService{

    @Override
    public BigDecimal calculate(String userId, BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount discount) {
        if(DiscountTypeEnum.TAG.equals(discount.getDiscountType()))
        {
            boolean isCrowdRange=filterTagId(userId,discount.getTagId());
            if(!isCrowdRange) return originalPrice;
        }

        return doCalculate(originalPrice,discount);
    }

    protected abstract BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount discount);

    private boolean filterTagId(String userId, String tagId) {
        return true;
    }

    protected BigDecimal isPriceBelowZero (BigDecimal discountPrice)
    {
        if(discountPrice.compareTo(BigDecimal.ZERO)<=0) return new BigDecimal("0.01");
        return discountPrice;
    }

}
