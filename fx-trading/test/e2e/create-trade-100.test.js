/**
 * Focused Browser Test: Create Trade with Amount 100 and Rate 1.16
 *
 * This test verifies that a user can:
 * 1. Log in as admin
 * 2. Create a trade with amount=100, exchangeRate=1.16
 * 3. See the trade appear in the list
 *
 * Run with: npm run test:simple-100
 * Or with Jest: npm test -- create-trade-100.test.js
 */

const puppeteer = require('puppeteer');

const BASE_URL = process.env.TEST_URL || 'http://localhost:8080';
const TEST_TIMEOUT = 30000;

describe('Create Trade with Amount 100 and Rate 1.16', () => {
    let browser;
    let page;

    beforeAll(async () => {
        browser = await puppeteer.launch({
            headless: true,
            args: ['--no-sandbox', '--disable-setuid-sandbox']
        });
        console.log('Browser launched for create trade test');
    }, TEST_TIMEOUT);

    afterAll(async () => {
        if (browser) {
            await browser.close();
            console.log('Browser closed');
        }
    });

    beforeEach(async () => {
        page = await browser.newPage();

        // Listen for console messages from the page
        page.on('console', msg => {
            const type = msg.type();
            if (type === 'error' || type === 'warning') {
                console.log(`[Browser ${type.toUpperCase()}]`, msg.text());
            }
        });

        // Listen for page errors
        page.on('pageerror', error => {
            console.error('[Page Error]', error.message);
        });

        console.log(`Navigating to ${BASE_URL}...`);
        // Note: The authentication is handled by the frontend JavaScript (app.js),
        // which includes the Authorization header in all API requests
        await page.goto(BASE_URL, { waitUntil: 'networkidle0' });
        console.log('Page loaded');
    }, TEST_TIMEOUT);

    afterEach(async () => {
        if (page) {
            await page.close();
        }
    });

    test('should log in as admin and create trade with amount 100 and rate 1.16', async () => {
        console.log('Test: Creating trade with amount=100, rate=1.16');

        // 1. Verify we're logged in and page loaded
        const title = await page.title();
        expect(title).toBe('FX Trading System');
        console.log('✓ Logged in successfully (page title verified)');

        // Check that the navbar shows "User: admin"
        const navbarText = await page.$eval('.navbar', el => el.textContent);
        expect(navbarText).toContain('admin');
        console.log('✓ User "admin" displayed in navbar');

        // 2. Wait for the form to be ready
        await page.waitForSelector('#tradeForm', { timeout: 5000 });
        console.log('✓ Trade form loaded');

        // 3. Fill out the form with the specified values
        // Leave base currency (EUR) and quote currency (USD) unchanged (default values)

        // Check current default values
        const baseCurrency = await page.$eval('#baseCurrency', el => el.value);
        const quoteCurrency = await page.$eval('#quoteCurrency', el => el.value);
        console.log(`  Base Currency (unchanged): ${baseCurrency}`);
        console.log(`  Quote Currency (unchanged): ${quoteCurrency}`);

        // Enter amount 100
        await page.type('#baseAmount', '100');
        console.log('  ✓ Entered amount: 100');

        // Enter exchange rate 1.16
        await page.type('#exchangeRate', '1.16');
        console.log('  ✓ Entered exchange rate: 1.16');

        // Set trade dates (default values should already be set, but ensure they're valid)
        const today = new Date().toISOString().split('T')[0];
        const valueDate = new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];

        await page.evaluate((td, vd) => {
            document.getElementById('tradeDate').value = td;
            document.getElementById('valueDate').value = vd;
        }, today, valueDate);
        console.log(`  ✓ Set trade date: ${today}`);
        console.log(`  ✓ Set value date: ${valueDate}`);

        // 4. Submit the form
        console.log('  Clicking "Create Trade" button...');

        // Count existing trades before submission
        const tradesBeforeCount = await page.evaluate(() => {
            const tbody = document.getElementById('tradesTableBody');
            const firstRow = tbody.querySelector('tr');
            if (firstRow && firstRow.textContent.includes('No trades found')) {
                return 0;
            }
            return tbody.querySelectorAll('tr').length;
        });
        console.log(`  Current trade count: ${tradesBeforeCount}`);

        // Submit and wait for success notification
        await Promise.all([
            page.waitForSelector('.toast.bg-success', { timeout: 10000 }),
            page.click('#tradeForm button[type="submit"]')
        ]);

        console.log('✓ Form submitted');

        // 5. Verify success notification
        const toastMessage = await page.$eval('#toastMessage', el => el.textContent);
        console.log(`✓ Success notification: "${toastMessage}"`);

        expect(toastMessage).toContain('created successfully');
        expect(toastMessage).toContain('FX-'); // Should contain trade reference

        // Extract trade reference from notification
        const tradeReferenceMatch = toastMessage.match(/FX-\d{8}-\d{4}/);
        const tradeReference = tradeReferenceMatch ? tradeReferenceMatch[0] : null;
        console.log(`✓ Trade reference: ${tradeReference}`);

        // 6. Wait for the table to refresh and show the new trade (not "No trades found")
        await page.waitForFunction(() => {
            const tbody = document.getElementById('tradesTableBody');
            const firstRow = tbody?.querySelector('tr');
            const text = firstRow?.textContent || '';
            // Wait until we see an FX- reference (not "No trades found")
            return text.includes('FX-');
        }, { timeout: 10000 });

        // 7. Verify the trade appears in the list
        console.log('Verifying trade appears in list...');

        const firstRowContent = await page.evaluate(() => {
            const tbody = document.getElementById('tradesTableBody');
            const firstRow = tbody.querySelector('tr');
            return {
                text: firstRow.textContent,
                reference: firstRow.querySelector('td:nth-child(1)')?.textContent.trim(),
                direction: firstRow.querySelector('td:nth-child(3)')?.textContent.trim(),
                pair: firstRow.querySelector('td:nth-child(4)')?.textContent.trim(),
                amount: firstRow.querySelector('td:nth-child(5)')?.textContent.trim(),
                rate: firstRow.querySelector('td:nth-child(6)')?.textContent.trim()
            };
        });

        console.log('First trade row:');
        console.log(`  Reference: ${firstRowContent.reference}`);
        console.log(`  Direction: ${firstRowContent.direction}`);
        console.log(`  Pair: ${firstRowContent.pair}`);
        console.log(`  Amount: ${firstRowContent.amount}`);
        console.log(`  Rate: ${firstRowContent.rate}`);

        // Verify the trade data matches what we entered
        expect(firstRowContent.reference).toContain('FX-');
        expect(firstRowContent.pair).toContain('EUR');
        expect(firstRowContent.pair).toContain('USD');
        expect(firstRowContent.amount).toContain('100'); // Should show formatted amount
        expect(firstRowContent.rate).toContain('1.16'); // Should show formatted rate

        console.log('✓ Trade appears in the list with correct values!');
        console.log('\n✅ TEST PASSED: Trade created and verified successfully!\n');
    }, TEST_TIMEOUT);
});
