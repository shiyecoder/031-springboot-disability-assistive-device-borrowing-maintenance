// 无头 Chrome 逐页截图：覆盖 5 个角色的全部页面，输出到 verify/shots/
import puppeteer from 'puppeteer-core'
import fs from 'node:fs'

const BASE = 'http://127.0.0.1:5174'
const OUT = new URL('../verify/shots/', import.meta.url).pathname
const CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
fs.mkdirSync(OUT, { recursive: true })

const USERS = {
  admin: 'admin123', zhangpg: '123456', wangcg: '123456', liwx: '123456', ligf: '123456'
}

let n = 0
const pad = (i) => String(i).padStart(2, '0')

async function shot(page, name) {
  n += 1
  const file = `${OUT}${pad(n)}_${name}.png`
  await new Promise((r) => setTimeout(r, 700))
  await page.screenshot({ path: file, fullPage: false })
  console.log('  ✓', file.split('/').pop())
}

async function login(page, username) {
  await page.goto(BASE + '/login', { waitUntil: 'networkidle2' })
  await page.evaluate(() => localStorage.clear())
  await page.goto(BASE + '/login', { waitUntil: 'networkidle2' })
  const inputs = await page.$$('input')
  await inputs[0].click({ clickCount: 3 })
  await inputs[0].evaluate((el) => { el.value = '' })
  await inputs[0].type(username)
  await inputs[1].click({ clickCount: 3 })
  await inputs[1].evaluate((el) => { el.value = '' })
  await inputs[1].type(USERS[username])
  await page.click('button.submit')
  await page.waitForFunction(() => location.pathname !== '/login', { timeout: 15000 })
  await new Promise((r) => setTimeout(r, 900))
}

const PAGES = [
  ['dashboard', 'dash_dashboard'],
  ['device/list', 'device_list'],
  ['device/inspection', 'inspection'],
  ['device/reservation', 'reservation'],
  ['borrow/list', 'borrow_list'],
  ['borrow/detail/1', 'borrow_detail'],
  ['borrow/assessment', 'assessment'],
  ['borrow/approval', 'approval'],
  ['borrow/handover', 'handover'],
  ['borrow/return', 'return_check'],
  ['repair/list', 'repair'],
  ['lifecycle/list', 'lifecycle'],
  ['feedback/list', 'feedback'],
  ['admin/rule', 'rule_config'],
  ['admin/user', 'user_list'],
  ['admin/menu', 'menu_list'],
  ['profile', 'profile']
]

const browser = await puppeteer.launch({
  executablePath: CHROME,
  headless: 'new',
  args: ['--headless=new', '--disable-gpu', '--no-sandbox', '--window-size=1440,900']
})
const page = await browser.newPage()
await page.setViewport({ width: 1440, height: 900 })
const errors = []
page.on('pageerror', (e) => errors.push('pageerror: ' + e.message))
page.on('console', (m) => { if (m.type() === 'error') errors.push('console: ' + m.text().slice(0, 160)) })

try {
  // 登录页（未登录态）
  await page.goto(BASE + '/login', { waitUntil: 'networkidle2' })
  await page.evaluate(() => localStorage.clear())
  await page.goto(BASE + '/login', { waitUntil: 'networkidle2' })
  await new Promise((r) => setTimeout(r, 800))
  await shot(page, 'login')

  // 管理员：全部页面
  await login(page, 'admin')
  for (const [path, name] of PAGES) {
    await page.goto(BASE + '/' + path, { waitUntil: 'networkidle2' })
    await new Promise((r) => setTimeout(r, 800))
    await shot(page, name)
  }

  // 详情抽屉：适配提醒依据
  await page.goto(BASE + '/lifecycle/list', { waitUntil: 'networkidle2' })
  await new Promise((r) => setTimeout(r, 900))
  const btns = await page.$$('button')
  for (const b of btns) {
    const t = await b.evaluate((el) => el.textContent.trim())
    if (t === '依据明细') { await b.click(); break }
  }
  await new Promise((r) => setTimeout(r, 900))
  await shot(page, 'lifecycle_drawer')

  // 评估员
  await login(page, 'zhangpg')
  await page.goto(BASE + '/borrow/assessment', { waitUntil: 'networkidle2' })
  await new Promise((r) => setTimeout(r, 800))
  await shot(page, 'role_assessor_assessment')
  await page.goto(BASE + '/borrow/approval', { waitUntil: 'networkidle2' })
  await new Promise((r) => setTimeout(r, 800))
  await shot(page, 'role_assessor_approval')

  // 仓管员
  await login(page, 'wangcg')
  for (const [path, name] of [['device/list', 'role_warehouse_device'], ['borrow/handover', 'role_warehouse_handover'], ['borrow/return', 'role_warehouse_return']]) {
    await page.goto(BASE + '/' + path, { waitUntil: 'networkidle2' })
    await new Promise((r) => setTimeout(r, 800))
    await shot(page, name)
  }

  // 维修员
  await login(page, 'liwx')
  await page.goto(BASE + '/repair/list', { waitUntil: 'networkidle2' })
  await new Promise((r) => setTimeout(r, 800))
  await shot(page, 'role_repairer_repair')

  // 借用人
  await login(page, 'ligf')
  await page.goto(BASE + '/borrow/create', { waitUntil: 'networkidle2' })
  await new Promise((r) => setTimeout(r, 800))
  await shot(page, 'role_borrower_create')
  await page.goto(BASE + '/dashboard', { waitUntil: 'networkidle2' })
  await new Promise((r) => setTimeout(r, 900))
  await shot(page, 'role_borrower_dashboard')
} catch (e) {
  console.error('FAILED:', e.message)
  errors.push('script: ' + e.message)
} finally {
  await browser.close()
  fs.writeFileSync(OUT + '../console-errors.txt', errors.length ? errors.join('\n') : '无前端控制台错误')
  console.log(`\n共 ${n} 张截图，控制台错误 ${errors.length} 条`)
  if (errors.length) console.log(errors.slice(0, 8).join('\n'))
}
