/**
 * 用本机 Chrome 登录两个前端并截图，顺便收集控制台错误。
 * 依赖：npm install（puppeteer-core，不会下载 Chromium）
 * 运行：node screenshot.js
 */
const path = require('node:path')
const puppeteer = require('puppeteer-core')

const CHROME = 'C:/Program Files/Google/Chrome/Application/chrome.exe'
const OUT = path.resolve(__dirname, '../../docs/screens')
const MALL = 'http://localhost:5180'
const ADMIN = 'http://localhost:5181'

const errors = []

function watch(page, tag) {
  page.on('pageerror', (err) => errors.push(`[${tag}] pageerror: ${err.message}`))
  page.on('console', (msg) => {
    if (msg.type() === 'error') {
      const text = msg.text()
      if (!text.includes('favicon') && !text.includes('DevTools')) {
        errors.push(`[${tag}] console: ${text}`)
      }
    }
  })
}

async function shot(page, name, fullPage = false) {
  await page.mouse.move(1, 1)
  await new Promise((r) => setTimeout(r, 900))
  await page.screenshot({ path: path.join(OUT, `${name}.png`), fullPage })
  console.log(`saved ${name}.png  <- ${page.url()}`)
}

async function main() {
  const browser = await puppeteer.launch({
    executablePath: CHROME,
    headless: 'new',
    defaultViewport: { width: 1440, height: 1000 },
    args: ['--disable-gpu', '--hide-scrollbars', '--no-sandbox']
  })

  const admin = await browser.newPage()
  watch(admin, 'admin')
  await admin.goto(`${ADMIN}/login`, { waitUntil: 'networkidle0' })
  await admin.type('input[placeholder="管理员账号"]', 'admin')
  await admin.type('input[placeholder="登录密码"]', '123456')
  await admin.click('button.submit')
  await admin.waitForFunction(() => location.pathname === '/dashboard', { timeout: 15000 })
  // 等 ElMessage 登录提示消失
  await new Promise((r) => setTimeout(r, 3400))
  await shot(admin, 'admin-dashboard')

  const adminPages = [
    ['products', 'admin-products'],
    ['categories', 'admin-categories'],
    ['seckill', 'admin-seckill'],
    ['orders', 'admin-orders'],
    ['refunds', 'admin-refunds'],
    ['users', 'admin-users'],
    ['roles', 'admin-roles'],
    ['logs', 'admin-logs']
  ]
  for (const [route, name] of adminPages) {
    await admin.goto(`${ADMIN}/${route}`, { waitUntil: 'networkidle0' })
    await new Promise((r) => setTimeout(r, 1000))
    await shot(admin, name)
  }

  // 打开权限分配抽屉截一张
  await admin.goto(`${ADMIN}/roles`, { waitUntil: 'networkidle0' })
  await new Promise((r) => setTimeout(r, 700))
  await admin.evaluate(() => {
    const buttons = [...document.querySelectorAll('button')]
    const target = buttons.find((b) => b.textContent.includes('分配权限'))
    target && target.click()
  })
  await new Promise((r) => setTimeout(r, 900))
  await shot(admin, 'admin-permissions')

  const mall = await browser.newPage()
  watch(mall, 'mall')
  await mall.goto(`${MALL}/login`, { waitUntil: 'networkidle0' })
  await mall.type('input[placeholder="请输入用户名"]', 'alice')
  await mall.type('input[placeholder="请输入密码"]', '123456')
  await mall.click('button.submit')
  await mall.waitForFunction(() => location.pathname === '/', { timeout: 15000 })
  await new Promise((r) => setTimeout(r, 3400))
  await shot(mall, 'mall-home-logged', true)

  for (const [route, name] of [
    ['/orders', 'mall-orders'],
    ['/order/1', 'mall-order-detail'],
    ['/profile', 'mall-profile'],
    ['/products', 'mall-products'],
    ['/seckill/2', 'mall-seckill-logged']
  ]) {
    await mall.goto(`${MALL}${route}`, { waitUntil: 'networkidle0' })
    await new Promise((r) => setTimeout(r, 1000))
    await shot(mall, name, route === '/orders' || route === '/order/1')
  }

  // 切换成 david，查看"退款审核中"的订单展示（直接调登录接口 + 注入 token，避免 UI 时序）
  await mall.goto(`${MALL}/login`, { waitUntil: 'networkidle0' })
  const davidLogin = await mall.evaluate(async () => {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'david', password: '123456' })
    })
    return res.json()
  })
  if (davidLogin.code === 0) {
    await mall.evaluate((data) => {
      localStorage.setItem('mall_token', data.token)
      localStorage.setItem(
        'mall_user',
        JSON.stringify({
          id: data.userId,
          username: data.username,
          nickname: data.nickname,
          avatar: data.avatar,
          roleType: data.roleType
        })
      )
    }, davidLogin.data)
    await mall.goto(`${MALL}/orders`, { waitUntil: 'networkidle0' })
    await new Promise((r) => setTimeout(r, 1000))
    await shot(mall, 'mall-orders-refund-pending', true)
  } else {
    console.log('david login failed:', davidLogin)
    process.exitCode = 1
  }

  await browser.close()

  console.log('\n===== console errors =====')
  if (errors.length === 0) {
    console.log('none')
  } else {
    errors.forEach((e) => console.log(e))
    process.exitCode = 1
  }
}

main().catch((e) => {
  console.error('screenshot failed:', e)
  process.exit(1)
})
