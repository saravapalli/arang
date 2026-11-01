import { chromium } from '@playwright/test'
const b = await chromium.launch()
const p = await b.newPage({ viewport:{width:1280,height:1000} })
await p.goto('http://localhost:8080/', { waitUntil:'networkidle' })
await p.waitForTimeout(1500)
await p.screenshot({ path:'/tmp/towns-landing.png' })
// type to filter
await p.locator('.town-search input').fill('wa')
await p.waitForTimeout(400)
await p.screenshot({ path:'/tmp/towns-filtered.png' })
await b.close(); console.log('done')
