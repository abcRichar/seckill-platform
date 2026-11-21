package com.seckill.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 轻量级结构升级：兼容已经跑过旧版本的数据库。
 * - 给 seckill_order 补退款字段
 * - 补退款相关权限点，并默认授予内置管理员角色
 *
 * 在 DataInitializer 之后执行，避免新库重复插入演示数据时主键冲突。
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class SchemaUpgradeRunner implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!hasColumn("seckill_order", "refund_status")) {
                jdbc.execute("ALTER TABLE seckill_order ADD COLUMN refund_status TINYINT NOT NULL DEFAULT 0 " +
                        "COMMENT '0无退款 1退款审核中 2已退款 3退款被拒' AFTER status");
                log.info("结构升级：seckill_order 新增 refund_status");
            }
            if (!hasColumn("seckill_order", "refund_time")) {
                jdbc.execute("ALTER TABLE seckill_order ADD COLUMN refund_time DATETIME DEFAULT NULL " +
                        "COMMENT '退款完成时间' AFTER refund_status");
                log.info("结构升级：seckill_order 新增 refund_time");
            }

            jdbc.update("INSERT IGNORE INTO sys_menu (id, perm_name, perm_code, type) VALUES " +
                    "(15, '退款查看', 'refund:list', 'MENU'), (16, '退款审核', 'refund:audit', 'BUTTON')");
            jdbc.update("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) " +
                    "SELECT r.id, m.id FROM sys_role r JOIN sys_menu m " +
                    "ON m.perm_code IN ('refund:list', 'refund:audit') WHERE r.role_code = 'ADMIN'");
            log.info("结构升级检查完成（退款字段/权限点）");
        } catch (Exception e) {
            log.warn("结构升级失败，请手动检查数据库: {}", e.getMessage());
        }
    }

    private boolean hasColumn(String table, String column) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Long.class, table, column);
        return count != null && count > 0;
    }
}
