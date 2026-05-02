package cn.bugstack.domain.trade.service.lock.factory;

import cn.bugstack.domain.trade.model.entity.GroupBuyActivityEntity;
import cn.bugstack.domain.trade.model.entity.TradeLockRuleCommandEntity;
import cn.bugstack.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import cn.bugstack.domain.trade.service.lock.filter.ActivityUsabeRuleFilter;
import cn.bugstack.domain.trade.service.lock.filter.TeamStockOccupyRuleFilter;
import cn.bugstack.domain.trade.service.lock.filter.UserTakeLimitRuleFilter;
import cn.bugstack.types.design.framework.link.model2.LinkArmory;
import cn.bugstack.types.design.framework.link.model2.chain.BusinessLinkedList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TradeRuleFilterFactory {

    @Bean("tradeRuleFilter")
    public BusinessLinkedList<TradeLockRuleCommandEntity,DynamicContext, TradeLockRuleFilterBackEntity>
    tradeRuleFilter(ActivityUsabeRuleFilter activityUsabeRuleFilter,
                    UserTakeLimitRuleFilter userTakeLimitRuleFilter,
                    TeamStockOccupyRuleFilter teamStockOccupyRuleFilter)
    {
        LinkArmory<TradeLockRuleCommandEntity,DynamicContext, TradeLockRuleFilterBackEntity> linkArmory
                =new LinkArmory<>("交易规则过滤链条"
                ,activityUsabeRuleFilter,
                userTakeLimitRuleFilter,
                teamStockOccupyRuleFilter);

        return linkArmory.getLogicLink();
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext
    {
        GroupBuyActivityEntity groupByActivity;
        private String teamStockKey = "group_buy_market_team_stock_key_";

        private GroupBuyActivityEntity groupBuyActivity;

        private Integer userTakeOrderCount;


        public String generateTeamStockKey(String teamId) {
            if (StringUtils.isBlank(teamId)) return null;
            return teamStockKey + groupBuyActivity.getActivityId() + "_" + teamId;
        }

        public String generateRecoveryTeamStockKey(String teamId) {
            if (StringUtils.isBlank(teamId)) return null;
            return teamStockKey + groupBuyActivity.getActivityId() + "_" + teamId + "_recovery";
        }
    }
}
