<template>
  <footer class="footer">
    <div class="container footer-top">
      <div class="brand-col">
        <div class="brand">
          <span class="mark"><el-icon><Lightning /></el-icon></span>
          <strong>秒杀集市</strong>
        </div>
        <p>用工程化的方式做秒杀：Redis 预扣、异步下单、订单状态机，每一行代码都可讲清楚。</p>
        <div class="badges">
          <span>正品保障</span>
          <span>极速发货</span>
          <span>7 天无理由</span>
        </div>
      </div>
      <div class="link-col">
        <h4>购物指南</h4>
        <a href="/products">全部商品</a>
        <a href="/#seckill">秒杀专场</a>
        <a href="/orders">我的订单</a>
      </div>
      <div class="link-col">
        <h4>关于我们</h4>
        <a href="javascript:;">项目介绍</a>
        <a href="http://localhost:9090/swagger-ui.html" target="_blank">接口文档</a>
        <a href="http://localhost:5181" target="_blank">管理后台</a>
      </div>
      <div class="link-col contact">
        <h4>联系客服</h4>
        <p class="phone">400-888-0000</p>
        <p>工作日 9:00 - 21:00</p>
        <p>service@seckill.demo</p>
      </div>
    </div>
    <div class="footer-bottom">
      <div class="container bottom-inner">
        <span>© 2026 秒杀集市 · 仅用于学习演示</span>
        <span>Vue 3 · Spring Boot 3 · MySQL · Redis</span>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  background: #11151c;
  color: #c9d1dc;
  margin-top: 60px;
}

.footer-top {
  display: grid;
  grid-template-columns: 2.2fr 1fr 1fr 1.2fr;
  gap: 40px;
  padding: 52px 0 42px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #fff;
  font-size: 19px;
  letter-spacing: 1px;
}

.mark {
  width: 36px;
  height: 36px;
  border-radius: 11px;
  background: linear-gradient(145deg, #ff3b30, #ff7a45);
  display: grid;
  place-items: center;
  color: #fff;
}

.brand-col p {
  font-size: 13px;
  line-height: 1.9;
  color: #8d98a8;
  max-width: 340px;
  margin: 18px 0;
}

.badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.badges span {
  font-size: 12px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  color: #aab4c2;
  padding: 4px 10px;
  border-radius: 999px;
}

.link-col {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.link-col h4 {
  color: #fff;
  font-size: 14px;
  margin: 0 0 4px;
  font-weight: 600;
}

.link-col a,
.link-col p {
  font-size: 13px;
  color: #8d98a8;
  margin: 0;
  transition: color 0.2s;
}

.link-col a:hover {
  color: #ff7a45;
}

.phone {
  font-size: 20px !important;
  font-weight: 700;
  color: #fff !important;
  letter-spacing: 0.5px;
}

.footer-bottom {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.bottom-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 0;
  font-size: 12px;
  color: #6b7686;
}

@media (max-width: 800px) {
  .footer-top {
    grid-template-columns: 1fr 1fr;
    gap: 28px;
  }
  .bottom-inner {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
