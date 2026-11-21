-- ============================================================
-- 秒杀平台表结构（幂等，可重复执行）
-- 库由 JDBC URL 的 createDatabaseIfNotExist=true 自动创建
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '登录名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
    nickname    VARCHAR(50)  DEFAULT '' COMMENT '昵称',
    avatar      VARCHAR(255) DEFAULT '' COMMENT '头像',
    phone       VARCHAR(20)  DEFAULT '' COMMENT '手机号',
    email       VARCHAR(100) DEFAULT '' COMMENT '邮箱',
    role_type   VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '身份类型 ADMIN/USER',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '用户表';

CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    role_name   VARCHAR(50) NOT NULL COMMENT '角色名',
    role_code   VARCHAR(50) NOT NULL COMMENT '角色编码',
    remark      VARCHAR(255) DEFAULT '',
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '角色表';

CREATE TABLE IF NOT EXISTS sys_menu (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    perm_name VARCHAR(50)  NOT NULL COMMENT '权限名',
    perm_code VARCHAR(100) NOT NULL COMMENT '权限编码',
    type      VARCHAR(20)  DEFAULT 'BUTTON' COMMENT 'MENU/BUTTON',
    PRIMARY KEY (id),
    UNIQUE KEY uk_perm_code (perm_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '权限表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id      BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '用户-角色';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id      BIGINT NOT NULL AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '角色-权限';

CREATE TABLE IF NOT EXISTS category (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL COMMENT '分类名',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '商品分类';

CREATE TABLE IF NOT EXISTS product (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    category_id    BIGINT        DEFAULT NULL,
    name           VARCHAR(100)  NOT NULL COMMENT '商品名',
    subtitle       VARCHAR(200)  DEFAULT '' COMMENT '副标题',
    cover          VARCHAR(255)  DEFAULT '' COMMENT '封面图',
    images         TEXT COMMENT '轮播图，逗号分隔',
    price          DECIMAL(10, 2) NOT NULL COMMENT '售价',
    original_price DECIMAL(10, 2) DEFAULT NULL COMMENT '划线价',
    stock          INT           NOT NULL DEFAULT 0 COMMENT '库存',
    sales          INT           NOT NULL DEFAULT 0 COMMENT '销量',
    status         TINYINT       NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
    description    TEXT COMMENT '详情',
    create_time    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_category (category_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '商品表';

CREATE TABLE IF NOT EXISTS seckill_activity (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    product_id     BIGINT        NOT NULL COMMENT '商品ID',
    title          VARCHAR(100)  NOT NULL COMMENT '活动标题',
    seckill_price  DECIMAL(10, 2) NOT NULL COMMENT '秒杀价',
    stock          INT           NOT NULL COMMENT '剩余库存（DB 为准）',
    initial_stock  INT           NOT NULL COMMENT '初始库存',
    limit_per_user INT           NOT NULL DEFAULT 1 COMMENT '每人限购',
    start_time     DATETIME      NOT NULL,
    end_time       DATETIME      NOT NULL,
    status         TINYINT       NOT NULL DEFAULT 0 COMMENT '0草稿 1上架 2结束',
    seckill_path   VARCHAR(64)   DEFAULT NULL COMMENT '隐藏秒杀链接',
    create_time    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_status_time (status, start_time, end_time),
    KEY idx_product (product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '秒杀活动';

CREATE TABLE IF NOT EXISTS seckill_order (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    order_no      VARCHAR(40)   NOT NULL COMMENT '订单号',
    user_id       BIGINT        NOT NULL,
    activity_id   BIGINT        NOT NULL,
    product_id    BIGINT        NOT NULL,
    product_name  VARCHAR(100)  DEFAULT '' COMMENT '商品名快照',
    product_cover VARCHAR(255)  DEFAULT '' COMMENT '封面快照',
    seckill_price DECIMAL(10, 2) DEFAULT 0.00,
    quantity      INT           NOT NULL DEFAULT 1,
    status        TINYINT       NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已取消 3已完成 4已退款',
    refund_status TINYINT       NOT NULL DEFAULT 0 COMMENT '0无退款 1退款审核中 2已退款 3退款被拒',
    refund_time   DATETIME      DEFAULT NULL COMMENT '退款完成时间',
    pay_time      DATETIME      DEFAULT NULL,
    cancel_time   DATETIME      DEFAULT NULL,
    create_time   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_user_activity (user_id, activity_id),
    KEY idx_user_status (user_id, status),
    KEY idx_activity (activity_id),
    KEY idx_status_create (status, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '秒杀订单';

CREATE TABLE IF NOT EXISTS seckill_refund (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    refund_no     VARCHAR(40)   NOT NULL COMMENT '退款单号',
    order_id      BIGINT        NOT NULL COMMENT '订单ID',
    order_no      VARCHAR(40)   NOT NULL COMMENT '订单号快照',
    user_id       BIGINT        NOT NULL,
    activity_id   BIGINT        DEFAULT NULL,
    product_id    BIGINT        DEFAULT NULL,
    product_name  VARCHAR(100)  DEFAULT '',
    product_cover VARCHAR(255)  DEFAULT '',
    refund_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
    reason        VARCHAR(100)  NOT NULL COMMENT '退款原因',
    description   VARCHAR(300)  DEFAULT '' COMMENT '补充说明',
    status        TINYINT       NOT NULL DEFAULT 0 COMMENT '0待审核 1已通过 2已拒绝',
    audit_by      BIGINT        DEFAULT NULL,
    audit_name    VARCHAR(50)   DEFAULT '',
    audit_time    DATETIME      DEFAULT NULL,
    audit_remark  VARCHAR(255)  DEFAULT '',
    refund_time   DATETIME      DEFAULT NULL COMMENT '退款完成时间',
    create_time   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_no (refund_no),
    UNIQUE KEY uk_order (order_id),
    KEY idx_status (status),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '退款申请单';

CREATE TABLE IF NOT EXISTS sys_operation_log (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       DEFAULT NULL,
    username   VARCHAR(50)  DEFAULT '',
    module     VARCHAR(50)  DEFAULT '',
    action     VARCHAR(100) DEFAULT '',
    method     VARCHAR(200) DEFAULT '',
    params     TEXT,
    ip         VARCHAR(50)  DEFAULT '',
    duration   BIGINT       DEFAULT 0 COMMENT '耗时ms',
    status     TINYINT      DEFAULT 1 COMMENT '1成功 0失败',
    error_msg  VARCHAR(500) DEFAULT '',
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '操作日志';
