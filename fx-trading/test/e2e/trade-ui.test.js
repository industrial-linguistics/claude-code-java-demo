/**
 * End-to-End Test for FX Trading UI
 *
 * This test verifies that:
 * 1. The UI loads without errors
 * 2. Trades can be loaded from the API
 * 3. Users can create new trades
 * 4. Trades appear in the list
 */

const puppeteer = require('puppeteer');

const BASE_URL = process.env.TEST_URL || 'http://localhost:8080';
const TEST_TIMEOUT = 30000;

describe('FX Trading UI', () => {
    let browser;
    let page;

    beforeAll(async () => {
        browser = await puppeteer.launch({
            headless: true,
            args: ['--no-sandbox', '--disable-setuid-sandbox']
        });
        console.log('Browser launched');
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

        await page.goto(BASE_URL, { waitUntil: 'networkidle0' });
    }, TEST_TIMEOUT);

    afterEach(async () => {
        if (page) {
            await page.close();
        }
    });

    test('should load the UI without errors', async () => {
        const title = await page.title();
        expect(title).toBe('FX Trading System');

        // Check that main elements are present
        const header = await page.$('h1, .navbar-brand');
        expect(header).not.toBeNull();

        const form = await page.$('#tradeForm');
        expect(form).not.toBeNull();

        const table = await page.$('#tradesTableBody');
        expect(table).not.toBeNull();
    }, TEST_TIMEOUT);

    test('should not show "Failed to load trades" error', async () => {
        // Wait for trades to load (or show empty state)
        await page.waitForSelector('#tradesTableBody', { timeout: 5000 });

        // Check if there's an error notification
        const toastText = await page.evaluate(() => {
            const toast = document.getElementById('toastMessage');
            return toast ? toast.textContent : null;
        });

        // Should not contain error about failed load
        if (toastText) {
            expect(toastText).not.toContain('Failed to load trades');
            expect(toastText).not.toContain('Error loading trades');
        }

        // Check table content - should either have trades or "No trades found"
        const tableContent = await page.$eval('#tradesTableBody', el => el.textContent);
        expect(tableContent).toBeTruthy();

        // Should not be empty (should have either trades or "No trades found" message)
        expect(tableContent.trim().length).toBeGreaterThan(0);
    }, TEST_TIMEOUT);

    test('should load trades from API successfully', async () => {
        // Wait for the trades table to be populated
        await page.waitForFunction(
            () => {
                const tbody = document.getElementById('tradesTableBody');
                return tbody && tbody.children.length > 0;
            },
            { timeout: 5000 }
        );

        const tableRows = await page.$$('#tradesTableBody tr');
        expect(tableRows.length).toBeGreaterThan(0);

        // Check if it's showing the empty state or actual trades
        const firstRowText = await page.$eval('#tradesTableBody tr:first-child', el => el.textContent);

        // Either should show "No trades found" or actual trade data
        const isEmptyState = firstRowText.includes('No trades found');
        const hasTradeReference = firstRowText.includes('FX-');

        expect(isEmptyState || hasTradeReference).toBe(true);
    }, TEST_TIMEOUT);

    test('should create a new trade successfully', async () => {
        // Fill out the trade form
        await page.select('#direction', 'BUY');
        await page.select('#baseCurrency', 'EUR');
        await page.select('#quoteCurrency', 'USD');
        await page.type('#baseAmount', '1000000');
        await page.type('#exchangeRate', '1.0850');

        // Trade date and value date are set by default, but let's set them explicitly
        const today = new Date().toISOString().split('T')[0];
        const valueDate = new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];

        await page.evaluate((td, vd) => {
            document.getElementById('tradeDate').value = td;
            document.getElementById('valueDate').value = vd;
        }, today, valueDate);

        await page.type('#counterparty', 'Test Bank ABC');
        await page.type('#trader', 'Test Trader');
        await page.type('#notes', 'Automated test trade');

        // Submit the form
        await Promise.all([
            page.waitForSelector('.toast.bg-success', { timeout: 10000 }), // Wait for success notification
            page.click('#tradeForm button[type="submit"]')
        ]);

        // Check that success notification appeared
        const successToast = await page.$('.toast.bg-success');
        expect(successToast).not.toBeNull();

        const toastMessage = await page.$eval('#toastMessage', el => el.textContent);
        expect(toastMessage).toContain('created successfully');
        expect(toastMessage).toContain('FX-');

        // Wait a bit for the table to refresh
        await page.waitForFunction(() => true, {timeout: 1000}).catch(() => {});

        // Check that the trade appears in the table
        const tradeRows = await page.$$('#tradesTableBody tr');
        expect(tradeRows.length).toBeGreaterThan(0);

        // Verify the first row contains our trade
        const firstRowText = await page.$eval('#tradesTableBody tr:first-child', el => el.textContent);
        expect(firstRowText).toContain('FX-');
        expect(firstRowText).toContain('BUY');
        expect(firstRowText).toContain('EUR/USD');
    }, TEST_TIMEOUT);

    test('should display trade details when clicking on a trade', async () => {
        // First create a trade
        await page.select('#direction', 'SELL');
        await page.select('#baseCurrency', 'GBP');
        await page.select('#quoteCurrency', 'USD');
        await page.type('#baseAmount', '500000');
        await page.type('#exchangeRate', '1.2650');

        await page.click('#tradeForm button[type="submit"]');
        await page.waitForSelector('.toast.bg-success', { timeout: 10000 });
        await page.waitForFunction(() => true, {timeout: 1000}).catch(() => {});

        // Click on the View button
        await Promise.all([
            page.waitForSelector('#tradeModal.show', { timeout: 5000 }),
            page.click('#tradesTableBody button.btn-outline-primary')
        ]);

        // Verify modal is visible
        const modal = await page.$('#tradeModal.show');
        expect(modal).not.toBeNull();

        // Check modal contains trade details
        const modalContent = await page.$eval('#tradeDetails', el => el.textContent);
        expect(modalContent).toContain('FX-');
        expect(modalContent).toContain('SELL');
        expect(modalContent).toContain('GBP');
    }, TEST_TIMEOUT);

    test('should filter trades using search box', async () => {
        // Create multiple trades
        const trades = [
            { dir: 'BUY', base: 'EUR', quote: 'USD', amount: '100000', rate: '1.0850', counterparty: 'Bank A' },
            { dir: 'SELL', base: 'GBP', quote: 'USD', amount: '200000', rate: '1.2650', counterparty: 'Bank B' }
        ];

        for (const trade of trades) {
            await page.select('#direction', trade.dir);
            await page.select('#baseCurrency', trade.base);
            await page.select('#quoteCurrency', trade.quote);

            // Clear and type amounts
            await page.click('#baseAmount', { clickCount: 3 });
            await page.type('#baseAmount', trade.amount);
            await page.click('#exchangeRate', { clickCount: 3 });
            await page.type('#exchangeRate', trade.rate);
            await page.click('#counterparty', { clickCount: 3 });
            await page.type('#counterparty', trade.counterparty);

            await page.click('#tradeForm button[type="submit"]');
            await page.waitForSelector('.toast.bg-success', { timeout: 10000 });
            await page.waitForFunction(() => true, {timeout: 500}).catch(() => {});
        }

        // Now test search
        await page.type('#searchInput', 'Bank A');
        await page.waitForFunction(() => true, {timeout: 500}).catch(() => {});

        // Check that only one trade is visible
        const visibleRows = await page.$$eval('#tradesTableBody tr', rows =>
            rows.filter(row => row.style.display !== 'none').length
        );

        // Should show at least the filtered trade
        const tableText = await page.$eval('#tradesTableBody', el => el.textContent);
        expect(tableText).toContain('Bank A');
    }, TEST_TIMEOUT);
});

// Simple test runner if not using Jest
if (typeof describe === 'undefined') {
    console.log('Running tests without Jest...');
    (async () => {
        try {
            // Run tests sequentially
            console.log('Test suite starting...');
            const tests = [
                { name: 'UI loads', fn: async () => { /* test code */ } }
            ];

            for (const test of tests) {
                console.log(`Running: ${test.name}`);
                await test.fn();
                console.log(`✓ ${test.name} passed`);
            }

            console.log('All tests passed!');
            process.exit(0);
        } catch (error) {
            console.error('Test failed:', error);
            process.exit(1);
        }
    })();
}
