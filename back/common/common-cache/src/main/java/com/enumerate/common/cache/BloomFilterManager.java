package com.enumerate.common.cache;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

/**
 * 布隆过滤器管理器
 *
 * 用于多级缓存体系的最外层防御，在查询 L1/L2/DB 前快速判断 key 是否存在。
 * 主要防御场景：缓存穿透 —— 恶意请求不存在的 ID 时，无需查库直接返回。
 *
 * 初始化流程：
 *   @PostConstruct 时从 articles 表加载全部 ID 到过滤器
 *
 * 更新策略：
 *   put(id) — 文章创建时调用
 *   不支持删除（标准布隆无删除操作；假阳性最多多查一次 DB 返回 404，无害）
 *
 * 配置：
 *   bloomfilter.expected-insertions — 预估容量（默认 100_000）
 *   bloomfilter.fpp — 期望假阳性率（默认 0.01）
 */
@Slf4j
@Component
public class BloomFilterManager {

    private BloomFilter<Long> bloomFilter;

    @Value("${bloomfilter.expected-insertions:100000}")
    private int expectedInsertions;

    @Value("${bloomfilter.fpp:0.01}")
    private double fpp;

    private final DataSource dataSource;

    public BloomFilterManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() {
        this.bloomFilter = BloomFilter.create(Funnels.longFunnel(), expectedInsertions, fpp);

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        try {
            List<Long> ids = jdbc.queryForList("SELECT id FROM articles", Long.class);
            int count = 0;
            for (Long id : ids) {
                bloomFilter.put(id);
                count++;
            }
            log.info("布隆过滤器初始化完成: 已加载 {} 个文章 ID, 配置: expectedInsertions={}, fpp={}",
                    count, expectedInsertions, fpp);
        } catch (Exception e) {
            log.warn("布隆过滤器初始化时加载文章 ID 失败, 过滤器为空: {}", e.getMessage());
        }
    }

    /**
     * 判断 ID 是否可能存在（false = 一定不存在）
     */
    public boolean mightContain(Long id) {
        if (id == null) return false;
        return bloomFilter.mightContain(id);
    }

    /**
     * 将 ID 加入布隆过滤器
     */
    public void put(Long id) {
        if (id != null) {
            bloomFilter.put(id);
        }
    }
}
