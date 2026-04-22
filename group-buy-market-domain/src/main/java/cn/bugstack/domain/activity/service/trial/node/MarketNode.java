package cn.bugstack.domain.activity.service.trial.node;

import cn.bugstack.domain.activity.model.entity.MarketProductEntity;
import cn.bugstack.domain.activity.model.entity.TrialBalanceEntity;

import cn.bugstack.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.bugstack.domain.activity.model.valobj.SkuVO;
import cn.bugstack.domain.activity.service.discount.IDiscountCalculateService;
import cn.bugstack.domain.activity.service.trial.AbstractGroupBuyMarketSupport;
import cn.bugstack.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;

import cn.bugstack.domain.activity.service.trial.thread.QueryGroupBuyActivityDiscountVOThreadTask;
import cn.bugstack.domain.activity.service.trial.thread.QuerySkuVOFromDBThreadTask;
import cn.bugstack.types.design.framework.tree.StrategyHandler;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 营销优惠节点
 * @create 2024-12-14 14:30
 */
@Slf4j
@Service
public class MarketNode extends AbstractGroupBuyMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private TagNode tagNode;

    @Resource
    private ErrorNode errorNode;

    @Resource
    Map<String, IDiscountCalculateService> discountCalculateServiceMap;

    @Override
    protected void multiThread(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) {
        QueryGroupBuyActivityDiscountVOThreadTask queryGroupBuyActivityDiscountVOThreadTask = new QueryGroupBuyActivityDiscountVOThreadTask(requestParameter.getSource(), requestParameter.getChannel(), requestParameter.getGoodsId(), requestParameter.getActivityId(), repository);
        FutureTask<GroupBuyActivityDiscountVO> groupBuyActivityDiscountVOFutureTask = new FutureTask<>(queryGroupBuyActivityDiscountVOThreadTask);
        threadPoolExecutor.execute(groupBuyActivityDiscountVOFutureTask);

        QuerySkuVOFromDBThreadTask querySkuVOFromDBThreadTask = new QuerySkuVOFromDBThreadTask(requestParameter.getGoodsId(), repository);
        FutureTask<SkuVO> skuVOFutureTask = new FutureTask<>(querySkuVOFromDBThreadTask);
        threadPoolExecutor.execute(skuVOFutureTask);

        try {
            dynamicContext.setGroupBuyActivityDiscountVO(groupBuyActivityDiscountVOFutureTask.get(timeout,TimeUnit.SECONDS));
            dynamicContext.setSkuVO(skuVOFutureTask.get(timeout,TimeUnit.SECONDS));
        } catch (TimeoutException e) {
            // 超时异常：通常是数据库慢查询或者并发太高，线程池排队了
            log.error("拼团试算多线程数据加载超时", e);
            throw new AppException(ResponseCode.UN_ERROR.getCode(), "系统试算超时，请稍后再试");

        } catch (ExecutionException e) {
            // 执行异常：说明底层 Repository 查数据库时出错了
            log.error("拼团试算多线程数据加载失败", e);
            throw new AppException(ResponseCode.UN_ERROR.getCode(), "数据加载异常");

        } catch (InterruptedException e) {
            // 中断异常：线程被意外杀死
            Thread.currentThread().interrupt(); // 保持中断状态
            log.error("拼团试算多线程数据加载中断", e);
            throw new AppException(ResponseCode.UN_ERROR.getCode(), "系统响应中断");
        }
    }

    /**
     * 业务逻辑受理
     *
     * @param requestParameter
     * @param dynamicContext
     * @return
     * @throws Exception
     */
    @Override
    protected TrialBalanceEntity doApply(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        GroupBuyActivityDiscountVO groupBuyActivityDiscountVO = dynamicContext.getGroupBuyActivityDiscountVO();
        if(null==groupBuyActivityDiscountVO) router(requestParameter,dynamicContext);
        GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount = groupBuyActivityDiscountVO.getGroupBuyDiscount();
        String marketPlan = groupBuyDiscount.getMarketPlan();

        SkuVO skuVO = dynamicContext.getSkuVO();
        if(null==groupBuyDiscount || null==skuVO) return router(requestParameter,dynamicContext);

        IDiscountCalculateService discountCalculateService = discountCalculateServiceMap.get(marketPlan);
        if (null == discountCalculateService) {
            log.info("不存在{}类型的折扣计算服务，支持类型为:{}", marketPlan, JSON.toJSONString(discountCalculateServiceMap.keySet()));
            throw new AppException(ResponseCode.E0001.getCode(), ResponseCode.E0001.getInfo());
        }
        BigDecimal payPrice = discountCalculateService.calculate(requestParameter.getUserId(), skuVO.getOriginalPrice(), groupBuyDiscount);
        dynamicContext.setDeductionPrice(skuVO.getOriginalPrice().subtract(payPrice));
        dynamicContext.setPayPrice(payPrice);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) {
        if(null==dynamicContext.getGroupBuyActivityDiscountVO() || null==dynamicContext.getSkuVO() || null==dynamicContext.getDeductionPrice())
        {
            return errorNode;
        }
        return tagNode;
    }
}
