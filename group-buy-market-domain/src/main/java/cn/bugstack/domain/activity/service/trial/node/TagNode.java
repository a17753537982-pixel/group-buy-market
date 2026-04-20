package cn.bugstack.domain.activity.service.trial.node;

import cn.bugstack.domain.activity.model.entity.MarketProductEntity;
import cn.bugstack.domain.activity.model.entity.TrialBalanceEntity;
import cn.bugstack.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.bugstack.domain.activity.service.trial.AbstractGroupBuyMarketSupport;
import cn.bugstack.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import cn.bugstack.types.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class TagNode extends AbstractGroupBuyMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    @Resource
    private EndNode endNode;
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
        //获取营销配置 判断可见性和可参与配置
        GroupBuyActivityDiscountVO groupBuyActivityDiscountVO = dynamicContext.getGroupBuyActivityDiscountVO();
        boolean enable = groupBuyActivityDiscountVO.isEnable();
        boolean visible = groupBuyActivityDiscountVO.isVisible();

        //进行人群标签过滤
        String userId = requestParameter.getUserId();
        String tagId = dynamicContext.getGroupBuyActivityDiscountVO().getTagId();
        //查看是否在能通过标签过滤
        boolean tagCustomer = repository.isTagCustomer(tagId,userId);

        enable=enable || tagCustomer;
        visible=visible || tagCustomer;
        dynamicContext.setVisible(visible);
        dynamicContext.setEnable(enable);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) {
        return endNode;
    }
}
