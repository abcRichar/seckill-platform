package com.seckill.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 首次启动初始化演示数据。
 * 统一密码 123456（BCrypt 存储），商品图片使用 uploads/seed 下的本地 SVG，离线也能看。
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Long userCount = jdbc.queryForObject("SELECT COUNT(*) FROM sys_user", Long.class);
        if (userCount != null && userCount > 0) {
            log.info("检测到已有数据，跳过初始化");
            return;
        }
        log.info("开始初始化演示数据...");
        String password = passwordEncoder.encode("123456");

        jdbc.update("INSERT INTO sys_role (id, role_name, role_code, remark) VALUES (?,?,?,?)",
                1L, "超级管理员", "ADMIN", "拥有全部权限");
        jdbc.update("INSERT INTO sys_role (id, role_name, role_code, remark) VALUES (?,?,?,?)",
                2L, "普通用户", "USER", "商城用户");

        List<Object[]> menus = List.of(
                new Object[]{1L, "数据看板", "dashboard:view", "MENU"},
                new Object[]{2L, "商品查看", "product:list", "MENU"},
                new Object[]{3L, "商品编辑", "product:edit", "BUTTON"},
                new Object[]{4L, "分类管理", "category:manage", "MENU"},
                new Object[]{5L, "秒杀查看", "seckill:list", "MENU"},
                new Object[]{6L, "秒杀编辑", "seckill:edit", "BUTTON"},
                new Object[]{7L, "订单查看", "order:list", "MENU"},
                new Object[]{8L, "订单操作", "order:edit", "BUTTON"},
                new Object[]{9L, "订单导出", "order:export", "BUTTON"},
                new Object[]{10L, "用户查看", "user:list", "MENU"},
                new Object[]{11L, "用户操作", "user:edit", "BUTTON"},
                new Object[]{12L, "角色权限", "role:manage", "MENU"},
                new Object[]{13L, "操作日志", "log:list", "MENU"},
                new Object[]{14L, "全部权限", "*", "BUTTON"},
                new Object[]{15L, "退款查看", "refund:list", "MENU"},
                new Object[]{16L, "退款审核", "refund:audit", "BUTTON"});
        for (Object[] menu : menus) {
            jdbc.update("INSERT INTO sys_menu (id, perm_name, perm_code, type) VALUES (?,?,?,?)", menu);
        }
        for (long menuId = 1; menuId <= 16; menuId++) {
            jdbc.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?,?)", 1L, menuId);
        }

        insertUser(1L, "admin", password, "平台管理员", "/uploads/seed/avatar-admin.svg",
                "13800000001", "admin@seckill.com", "ADMIN", 1L);
        insertUser(2L, "alice", password, "橙子", "/uploads/seed/avatar-alice.svg",
                "13800000002", "alice@seckill.com", "USER", 2L);
        insertUser(3L, "bob", password, "小北", "/uploads/seed/avatar-bob.svg",
                "13800000003", "bob@seckill.com", "USER", 2L);
        insertUser(4L, "carol", password, "卡卡", "/uploads/seed/avatar-carol.svg",
                "13800000004", "carol@seckill.com", "USER", 2L);
        insertUser(5L, "david", password, "阿杰", "/uploads/seed/avatar-david.svg",
                "13800000005", "david@seckill.com", "USER", 2L);
        insertUser(6L, "emma", password, "木木", "/uploads/seed/avatar-emma.svg",
                "13800000006", "emma@seckill.com", "USER", 2L);

        jdbc.update("INSERT INTO category (id, name, sort) VALUES (?,?,?)", 1L, "手机数码", 1);
        jdbc.update("INSERT INTO category (id, name, sort) VALUES (?,?,?)", 2L, "电脑办公", 2);
        jdbc.update("INSERT INTO category (id, name, sort) VALUES (?,?,?)", 3L, "品质生活", 3);
        jdbc.update("INSERT INTO category (id, name, sort) VALUES (?,?,?)", 4L, "潮流配件", 4);

        insertProduct(1L, 1L, "星曜 X1 Pro 5G 手机",
                "骁龙旗舰芯片 · 2K 直屏 · 100W 快充", "/uploads/seed/phone.svg",
                new BigDecimal("5999.00"), new BigDecimal("6999.00"), 200, 130,
                "6.78 英寸 2K 直屏，旗舰处理器，一亿像素主摄，5000mAh 大电池。\n支持 100W 有线快充，30 分钟回血 80%。");
        insertProduct(2L, 2L, "极光 Air 14 笔记本",
                "1.2kg 轻薄机身 · 2.8K OLED · 32G 内存", "/uploads/seed/laptop.svg",
                new BigDecimal("6499.00"), new BigDecimal("7499.00"), 80, 46,
                "14 英寸 2.8K OLED 屏幕，色彩精准，适合设计与移动办公。\n全金属机身，重量仅 1.2kg。");
        insertProduct(3L, 4L, "声浪 Pro 主动降噪耳机",
                "48dB 深度降噪 · 45 小时续航", "/uploads/seed/headphones.svg",
                new BigDecimal("899.00"), new BigDecimal("1299.00"), 300, 235,
                "混合主动降噪，通勤地铁也能安静听歌。\n支持 LDAC 高清编码，佩戴轻巧无压耳。");
        insertProduct(4L, 4L, "时光 S3 智能手表",
                "血氧心率监测 · 14 天续航", "/uploads/seed/watch.svg",
                new BigDecimal("1299.00"), new BigDecimal("1599.00"), 150, 87,
                "1.43 英寸 AMOLED 屏幕，支持 100+ 运动模式。\n全天候健康监测，续航最长 14 天。");
        insertProduct(5L, 3L, "醇香手冲咖啡礼盒",
                "精品豆组合 · 手冲器具一套", "/uploads/seed/coffee.svg",
                new BigDecimal("199.00"), new BigDecimal("299.00"), 500, 342,
                "精选三大产区精品咖啡豆，附赠手冲壶与滤杯。\n适合作为节日礼物。");
        insertProduct(6L, 1L, "声场 X 便携蓝牙音箱",
                "360° 环绕声 · IPX7 防水", "/uploads/seed/speaker.svg",
                new BigDecimal("399.00"), new BigDecimal("499.00"), 220, 110,
                "双单元立体声，户外防水防尘，露营氛围担当。\n支持双设备串联。");
        insertProduct(7L, 2L, "静音机械键盘 87 键",
                "热插拔 · 三模连接 · PBT 键帽", "/uploads/seed/keyboard.svg",
                new BigDecimal("499.00"), new BigDecimal("599.00"), 180, 95,
                "办公静音轴体，三模连接，支持全键热插拔。\nPBT 键帽耐磨不打油。");
        insertProduct(8L, 4L, "城市通勤双肩包",
                "16 寸电脑仓 · 防泼水面料", "/uploads/seed/backpack.svg",
                new BigDecimal("329.00"), new BigDecimal("429.00"), 260, 154,
                "简约通勤设计，独立电脑仓可放 16 寸笔记本。\n防泼水面料，通勤出差都合适。");

        LocalDateTime now = LocalDateTime.now();
        insertActivity(1L, 1L, "星曜 X1 Pro 限时秒杀", new BigDecimal("3999.00"),
                98, 100, now.minusHours(1), now.plusDays(7), 1);
        insertActivity(2L, 3L, "声浪 Pro 半价抢", new BigDecimal("449.00"),
                48, 50, now.minusHours(2), now.plusDays(7), 1);
        insertActivity(3L, 2L, "极光 Air 14 预约秒杀", new BigDecimal("5499.00"),
                30, 30, now.plusDays(1), now.plusDays(2), 1);
        insertActivity(4L, 4L, "时光 S3 昨日场次", new BigDecimal("799.00"),
                0, 20, now.minusDays(3), now.minusDays(2), 2);

        insertOrder("SK2024111800010001", 2L, 1L, 1L, "星曜 X1 Pro 限时秒杀",
                "/uploads/seed/phone.svg", new BigDecimal("3999.00"),
                1, 1, now.minusHours(3), now.minusHours(2).minusMinutes(50));
        insertOrder("SK2024111800010002", 3L, 1L, 1L, "星曜 X1 Pro 限时秒杀",
                "/uploads/seed/phone.svg", new BigDecimal("3999.00"),
                1, 1, now.minusHours(2), now.minusHours(1).minusMinutes(55));
        insertOrder("SK2024111800010003", 4L, 2L, 3L, "声浪 Pro 半价抢",
                "/uploads/seed/headphones.svg", new BigDecimal("449.00"),
                1, 2, now.minusHours(5), null);
        insertOrder("SK2024111800010004", 5L, 2L, 3L, "声浪 Pro 半价抢",
                "/uploads/seed/headphones.svg", new BigDecimal("449.00"),
                1, 1, now.minusHours(1), now.minusMinutes(55));
        insertOrder("SK2024111800010005", 6L, 2L, 3L, "声浪 Pro 半价抢",
                "/uploads/seed/headphones.svg", new BigDecimal("449.00"),
                1, 1, now.minusMinutes(40), now.minusMinutes(35));

        // 两笔退款演示：一笔待审核、一笔已退款
        Long davidOrderId = jdbc.queryForObject(
                "SELECT id FROM seckill_order WHERE order_no = ?", Long.class, "SK2024111800010004");
        Long emmaOrderId = jdbc.queryForObject(
                "SELECT id FROM seckill_order WHERE order_no = ?", Long.class, "SK2024111800010005");

        jdbc.update("UPDATE seckill_order SET refund_status = 1 WHERE id = ?", davidOrderId);
        jdbc.update("INSERT INTO seckill_refund (refund_no, order_id, order_no, user_id, activity_id, " +
                        "product_id, product_name, product_cover, refund_amount, reason, description, status, create_time) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,0,?)",
                "RF2024111800010001", davidOrderId, "SK2024111800010004", 5L, 2L, 3L,
                "声浪 Pro 半价抢", "/uploads/seed/headphones.svg", new BigDecimal("449.00"),
                "临时不需要了", "行程有变，麻烦帮忙退款", Timestamp.valueOf(now.minusMinutes(30)));

        jdbc.update("UPDATE seckill_order SET status = 4, refund_status = 2, refund_time = ? WHERE id = ?",
                Timestamp.valueOf(now.minusMinutes(20)), emmaOrderId);
        jdbc.update("INSERT INTO seckill_refund (refund_no, order_id, order_no, user_id, activity_id, " +
                        "product_id, product_name, product_cover, refund_amount, reason, description, status, " +
                        "audit_by, audit_name, audit_time, audit_remark, refund_time, create_time) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,1,1,'admin',?,?,?,?)",
                "RF2024111800010002", emmaOrderId, "SK2024111800010005", 6L, 2L, 3L,
                "声浪 Pro 半价抢", "/uploads/seed/headphones.svg", new BigDecimal("449.00"),
                "商品不符合预期", "耳机佩戴不太合适", Timestamp.valueOf(now.minusMinutes(20)),
                "已同意，原路退回", Timestamp.valueOf(now.minusMinutes(20)),
                Timestamp.valueOf(now.minusHours(1)));

        // 几条演示操作日志，方便直接看到日志页效果（真实操作会继续追加）
        insertDemoLog("秒杀活动", "上架活动", "AdminSeckillController.publish", "{\"id\":1}",
                42, now.minusHours(2));
        insertDemoLog("商品管理", "新增商品", "AdminProductController.create", "{\"name\":\"星曜 X1 Pro 5G 手机\"}",
                36, now.minusHours(3));
        insertDemoLog("权限管理", "分配角色权限", "AdminRoleController.assignMenus", "{\"menuIds\":[1,2,3,4,5]}",
                57, now.minusHours(5));
        insertDemoLog("退款管理", "通过退款", "AdminRefundController.approve", "{\"id\":2}",
                33, now.minusMinutes(20));

        log.info("演示数据初始化完成：admin/123456（管理端），alice/123456（用户端）");
    }

    private void insertUser(Long id, String username, String password, String nickname, String avatar,
                            String phone, String email, String roleType, Long roleId) {
        jdbc.update("INSERT INTO sys_user (id, username, password, nickname, avatar, phone, email, role_type, status) " +
                        "VALUES (?,?,?,?,?,?,?,?,1)",
                id, username, password, nickname, avatar, phone, email, roleType);
        jdbc.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?,?)", id, roleId);
    }

    private void insertProduct(Long id, Long categoryId, String name, String subtitle, String cover,
                               BigDecimal price, BigDecimal originalPrice, int stock, int sales,
                               String description) {
        jdbc.update("INSERT INTO product (id, category_id, name, subtitle, cover, images, price, " +
                        "original_price, stock, sales, status, description) VALUES (?,?,?,?,?,?,?,?,?,?,1,?)",
                id, categoryId, name, subtitle, cover, cover, price, originalPrice, stock, sales, description);
    }

    private void insertActivity(Long id, Long productId, String title, BigDecimal price,
                                int stock, int initialStock, LocalDateTime start,
                                LocalDateTime end, int status) {
        String path = UUID.randomUUID().toString().replace("-", "");
        jdbc.update("INSERT INTO seckill_activity (id, product_id, title, seckill_price, stock, " +
                        "initial_stock, limit_per_user, start_time, end_time, status, seckill_path) " +
                        "VALUES (?,?,?,?,?,?,1,?,?,?,?)",
                id, productId, title, price, stock, initialStock,
                Timestamp.valueOf(start), Timestamp.valueOf(end), status, path);
    }

    private void insertDemoLog(String module, String action, String method, String params,
                               long duration, LocalDateTime createTime) {
        jdbc.update("INSERT INTO sys_operation_log (user_id, username, module, action, method, params, " +
                        "ip, duration, status, create_time) VALUES (1, 'admin', ?, ?, ?, ?, '127.0.0.1', ?, 1, ?)",
                module, action, method, params, duration, Timestamp.valueOf(createTime));
    }

    private void insertOrder(String orderNo, Long userId, Long activityId, Long productId,
                             String productName, String cover, BigDecimal price, int quantity,
                             int status, LocalDateTime createTime, LocalDateTime payTime) {
        jdbc.update("INSERT INTO seckill_order (order_no, user_id, activity_id, product_id, product_name, " +
                        "product_cover, seckill_price, quantity, status, pay_time, cancel_time, create_time) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                orderNo, userId, activityId, productId, productName, cover, price, quantity, status,
                payTime == null ? null : Timestamp.valueOf(payTime),
                status == 2 ? Timestamp.valueOf(createTime.plusMinutes(15)) : null,
                Timestamp.valueOf(createTime));
    }
}
