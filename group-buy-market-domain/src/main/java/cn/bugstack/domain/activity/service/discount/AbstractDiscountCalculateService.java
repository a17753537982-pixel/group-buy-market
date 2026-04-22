package cn.bugstack.domain.activity.service.discount;

import cn.bugstack.domain.activity.adapter.repository.IActivityRepository;
import cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum;
import cn.bugstack.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Slf4j
public abstract class AbstractDiscountCalculateService implements IDiscountCalculateService{


    @Resource
    protected IActivityRepository repository;

    @Override
    public BigDecimal calculate(String userId, BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount discount) {
        if(DiscountTypeEnum.TAG.equals(discount.getDiscountType()))
        {
            boolean isCrowdRange=filterTagId(userId,discount.getTagId());
            if(!isCrowdRange) {
                log.info("折扣优惠计算人群拦截，userId:{}",userId);
                return originalPrice;
            }
        }

        return doCalculate(originalPrice,discount);
    }

    protected abstract BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount discount);

    private boolean filterTagId(String userId, String tagId) {
        return repository.isTagCustomer(tagId,userId);
    }

    protected BigDecimal isPriceBelowZero (BigDecimal discountPrice)
    {
        if(discountPrice.compareTo(BigDecimal.ZERO)<=0) return new BigDecimal("0.01");
        return discountPrice;
    }

}
