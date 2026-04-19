package cn.bugstack.domain.activity.service.discount.impl;

import cn.bugstack.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.bugstack.domain.activity.service.discount.AbstractDiscountCalculateService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service("ZJ")
public class ZJCalculateService extends AbstractDiscountCalculateService {
    @Override
    protected BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount discount) {
        String marketExpr = discount.getMarketExpr();
        BigDecimal discountPrice=originalPrice.subtract(new BigDecimal(marketExpr));
        return isPriceBelowZero(discountPrice);
    }
}
