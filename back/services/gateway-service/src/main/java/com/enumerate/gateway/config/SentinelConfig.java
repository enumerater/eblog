package com.enumerate.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

/**
 * Sentinel 网关规则初始化
 *
 * 限流策略:
 *  1. 读接口群组 (articles, search) — 高 QPS, 批量限流
 *  2. 写接口群组 (create, update, delete) — 低 QPS, 严格限流
 *  3. 热点文章参数限流 — 单个文章的 QPS 限制
 *
 * 生产环境建议规则推送到 Nacos 配置中心, 通过 DataSource 动态加载
 * 此处为本地初始化兜底规则
 */
@Slf4j
@Configuration
public class SentinelConfig {

    @PostConstruct
    public void init() {
        initApiGroups();
        initGatewayRules();
        log.info("Sentinel 网关规则初始化完成");
    }

    /**
     * 定义 API 分组
     */
    private void initApiGroups() {
        Set<ApiDefinition> apis = new HashSet<>();

        // ── 读接口群组 ──
        apis.add(new ApiDefinition("read-api-group")
                .setPredicateItems(new HashSet<>() {{
                    add(new ApiPathPredicateItem()
                            .setPattern("/api/articles")
                            .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
                    add(new ApiPathPredicateItem()
                            .setPattern("/api/articles-query/**"));
                    add(new ApiPathPredicateItem()
                            .setPattern("/api/search/**"));
                    add(new ApiPathPredicateItem()
                            .setPattern("/api/comments")
                            .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
                    add(new ApiPathPredicateItem()
                            .setPattern("/api/intelligence/**"));
                    add(new ApiPathPredicateItem()
                            .setPattern("/api/files/**"));
                }}));

        // ── 写接口群组 ──
        apis.add(new ApiDefinition("write-api-group")
                .setPredicateItems(new HashSet<>() {{
                    add(new ApiPathPredicateItem()
                            .setPattern("/api/articles/**")
                            .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
                    add(new ApiPathPredicateItem()
                            .setPattern("/api/drafts/**")
                            .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
                    add(new ApiPathPredicateItem()
                            .setPattern("/api/comments/**")
                            .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
                    add(new ApiPathPredicateItem()
                            .setPattern("/api/notifications/**")
                            .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
                }}));

        // ── Auth 群组 ──
        apis.add(new ApiDefinition("auth-api-group")
                .setPredicateItems(new HashSet<>() {{
                    add(new ApiPathPredicateItem()
                            .setPattern("/api/auth/**"));
                }}));

        GatewayApiDefinitionManager.loadApiDefinitions(apis);
    }

    /**
     * 定义网关流控规则
     */
    private void initGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        // 1. 读接口: 1000 QPS
        rules.add(new GatewayFlowRule("read-api-group")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(1000)
                .setIntervalSec(1)
                .setBurst(200)
                .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT));

        // 2. 写接口: 50 QPS (严格)
        rules.add(new GatewayFlowRule("write-api-group")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(50)
                .setIntervalSec(1)
                .setBurst(10)
                .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER));

        // 3. Auth 接口: 20 QPS (防刷)
        rules.add(new GatewayFlowRule("auth-api-group")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(20)
                .setIntervalSec(1)
                .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT));

        // 4. 热点文章参数限流 (对 article-service GET 请求按文章ID限流)
        rules.add(new GatewayFlowRule("/api/articles/**")
                .setCount(200)
                .setIntervalSec(1)
                .setBurst(50)
                .setParamItem(new GatewayParamFlowItem()
                        .setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_URL_PARAM)
                        .setFieldName("id")
                        .setMatchStrategy(SentinelGatewayConstants.PARAM_MATCH_STRATEGY_EXACT)));

        GatewayRuleManager.loadRules(rules);
        log.info("加载 {} 条网关限流规则", rules.size());
    }
}
