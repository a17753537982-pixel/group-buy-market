package cn.bugstack.domain.trade.service.factory;

import cn.bugstack.domain.trade.model.entity.GroupByActivityEntity;
import cn.bugstack.domain.trade.model.entity.TradeRuleCommandEntity;
import cn.bugstack.domain.trade.model.entity.TradeRuleFilterBackEntity;
import cn.bugstack.domain.trade.service.filter.ActivityUsabeRuleFilter;
import cn.bugstack.domain.trade.service.filter.UserTakeLimitRuleFilter;
import cn.bugstack.types.design.framework.link.model2.LinkArmory;
import cn.bugstack.types.design.framework.link.model2.chain.BusinessLinkedList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TradeRuleFilterFactory {

    @Bean("tradeRuleFilter")
    public BusinessLinkedList<TradeRuleCommandEntity,DynamicContext, TradeRuleFilterBackEntity>
    tradeRuleFilter(ActivityUsabeRuleFilter activityUsabeRuleFilter, UserTakeLimitRuleFilter userTakeLimitRuleFilter)
    {
        LinkArmory<TradeRuleCommandEntity,DynamicContext, TradeRuleFilterBackEntity> linkArmory
                =new LinkArmory<>("交易规则过滤链条",activityUsabeRuleFilter,userTakeLimitRuleFilter);

        return linkArmory.getLogicLink();
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext
    {
        GroupByActivityEntity groupByActivity;
    }
}
