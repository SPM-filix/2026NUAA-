package com.nuaa.recycle.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 统计聚合（API-STA-01～05）。模块层 Mapper 统一 Module 前缀，避免与核心 Mapper 重名。
 * 返回 Map 的 key 一律用别名显式写成驼峰，不依赖 map-underscore-to-camel-case。
 * 注意：只有以 &lt;script&gt; 开头的注解 SQL 走 XML 解析，其余注解里的比较符必须写成原生 &gt;= / &lt;。
 */
@Mapper
public interface ModuleStatsMapper {

    @Select("""
            SELECT COALESCE(SUM(CASE WHEN status = 'NORMAL' THEN weight ELSE 0 END), 0) AS totalWeight,
                   COALESCE(SUM(CASE WHEN status = 'NORMAL' THEN points ELSE 0 END), 0) AS totalPointsIssued
            FROM delivery_record
            """)
    Map<String, Object> deliveryTotals();

    @Select("""
            SELECT COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN points_cost ELSE 0 END), 0) AS totalPointsRedeemed,
                   COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completedOrderCount,
                   COALESCE(SUM(CASE WHEN status = 'PENDING' AND redeem_expire_at < NOW() THEN 1 ELSE 0 END), 0)
                       AS expiredPendingOrderCount
            FROM exchange_order
            """)
    Map<String, Object> exchangeTotals();

    @Select("""
            <script>
            SELECT d.category_id AS categoryId,
                   c.name AS categoryName,
                   COALESCE(SUM(d.weight), 0) AS weight,
                   COALESCE(SUM(d.points), 0) AS points,
                   COUNT(*) AS recordCount
            FROM delivery_record d
            LEFT JOIN recycle_category c ON c.id = d.category_id
            WHERE d.status = 'NORMAL'
            <if test="from != null">AND d.created_at &gt;= #{from}</if>
            <if test="to != null">AND d.created_at &lt; DATE_ADD(#{to}, INTERVAL 1 DAY)</if>
            GROUP BY d.category_id, c.name
            ORDER BY weight DESC
            </script>
            """)
    List<Map<String, Object>> categoryStats(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS statDate,
                   COALESCE(SUM(weight), 0) AS weight,
                   COALESCE(SUM(points), 0) AS points,
                   COUNT(*) AS deliveryCount
            FROM delivery_record
            WHERE status = 'NORMAL'
              AND created_at >= #{start}
              AND created_at < #{endExclusive}
            GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
            """)
    List<Map<String, Object>> deliveryTrend(@Param("start") LocalDateTime start,
                                            @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS statDate,
                   COUNT(*) AS orderCount
            FROM exchange_order
            WHERE created_at >= #{start}
              AND created_at < #{endExclusive}
            GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
            """)
    List<Map<String, Object>> orderTrend(@Param("start") LocalDateTime start,
                                         @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            SELECT o.product_id AS id,
                   COALESCE(p.name, '已删除商品') AS name,
                   COALESCE(SUM(o.quantity), 0) AS value,
                   COALESCE(SUM(o.points_cost), 0) AS extra
            FROM exchange_order o
            LEFT JOIN product p ON p.id = o.product_id
            WHERE o.status = 'COMPLETED'
            GROUP BY o.product_id, p.name
            ORDER BY value DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> topProducts(@Param("limit") int limit);

    @Select("""
            SELECT d.user_id AS id,
                   u.nickname AS name,
                   COALESCE(SUM(d.weight), 0) AS value,
                   COALESCE(SUM(d.points), 0) AS extra
            FROM delivery_record d
            LEFT JOIN `user` u ON u.id = d.user_id
            WHERE d.status = 'NORMAL'
            GROUP BY d.user_id, u.nickname
            ORDER BY value DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> topUsers(@Param("limit") int limit);

    @Select("""
            SELECT COALESCE(SUM(weight), 0) AS todayWeight,
                   COALESCE(SUM(points), 0) AS todayPoints,
                   COUNT(*) AS todayDeliveryCount
            FROM delivery_record
            WHERE recycler_id = #{recyclerId}
              AND status = 'NORMAL'
              AND created_at >= CURDATE()
              AND created_at < DATE_ADD(CURDATE(), INTERVAL 1 DAY)
            """)
    Map<String, Object> workbenchToday(@Param("recyclerId") Long recyclerId);

    @Select("""
            SELECT COUNT(*) AS todayRedeemCount
            FROM exchange_order
            WHERE verified_by = #{recyclerId}
              AND status = 'COMPLETED'
              AND verified_at >= CURDATE()
              AND verified_at < DATE_ADD(CURDATE(), INTERVAL 1 DAY)
            """)
    Map<String, Object> workbenchTodayRedeem(@Param("recyclerId") Long recyclerId);

    @Select("""
            SELECT s.name
            FROM recycler r
            LEFT JOIN site s ON s.id = r.site_id
            WHERE r.id = #{recyclerId}
            """)
    String workbenchSiteName(@Param("recyclerId") Long recyclerId);

    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS statDate,
                   COALESCE(SUM(weight), 0) AS weight
            FROM delivery_record
            WHERE recycler_id = #{recyclerId}
              AND status = 'NORMAL'
              AND created_at >= #{start}
              AND created_at < #{endExclusive}
            GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
            """)
    List<Map<String, Object>> recyclerWeightTrend(@Param("recyclerId") Long recyclerId,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("endExclusive") LocalDateTime endExclusive);

    /**
     * 本月本人在所属站点已通过审核回收员中的积分排名；无记录时返回 null。
     */
    @Select("""
            SELECT t.rankNo
            FROM (
                SELECT d.recycler_id AS recyclerId,
                       RANK() OVER (ORDER BY SUM(d.points) DESC) AS rankNo
                FROM delivery_record d
                JOIN recycler r ON r.id = d.recycler_id AND r.status = 'APPROVED'
                WHERE d.status = 'NORMAL'
                  AND r.site_id = (SELECT site_id FROM recycler WHERE id = #{recyclerId})
                  AND d.created_at >= DATE_FORMAT(CURDATE(), '%Y-%m-01')
                GROUP BY d.recycler_id
            ) t
            WHERE t.recyclerId = #{recyclerId}
            """)
    Integer monthRank(@Param("recyclerId") Long recyclerId);
}
