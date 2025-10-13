const puppeteer = require('puppeteer');

describe('FX Trading - Create AUD/USD Trade', () => {
    let browser;
    let page;
    const baseUrl = 'http://localhost:8080';

    beforeAll(async () => {
        browser = await puppeteer.launch({
            headless: true,
            args: ['--no-sandbox', '--disable-setuid-sandbox']
        });
    });

    afterAll(async () => {
        await browser.close();
    });

    beforeEach(async () => {
        page = await browser.newPage();

        // Set up HTTP Basic Authentication
        await page.authenticate({
            username: 'admin',
            password: 'changeme'
        });

        // Listen for console messages to capture errors
        page.on('console', msg => {
            if (msg.type() === 'error') {
                console.log('Browser Console Error:', msg.text());
            }
        });

        await page.goto(baseUrl, { waitUntil: 'networkidle2' });
    });

    afterEach(async () => {
        await page.close();
    });

    test('should create AUD/USD trade successfully', async () => {
        // Get today's date in YYYY-MM-DD format
        const today = new Date();
        const tradeDate = today.toISOString().split('T')[0];

        // Value date is T+2 (two business days later)
        const valueDate = new Date(today);
        valueDate.setDate(valueDate.getDate() + 2);
        const valueDateStr = valueDate.toISOString().split('T')[0];

        console.log('Filling out trade form...');
        console.log(`Trade Date: ${tradeDate}, Value Date: ${valueDateStr}`);

        // Fill out the form
        await page.select('#direction', 'BUY');
        await page.select('#baseCurrency', 'AUD');
        await page.select('#quoteCurrency', 'USD');
        await page.type('#baseAmount', '421.83');
        await page.type('#exchangeRate', '0.7');

        // Set date fields properly using evaluate to avoid input scrambling
        await page.evaluate((td, vd) => {
            document.getElementById('tradeDate').value = td;
            document.getElementById('valueDate').value = vd;
        }, tradeDate, valueDateStr);

        await page.type('#counterparty', 'JP Morgan');
        // Notes field is left empty as specified

        console.log('Submitting form...');

        // Set up a promise to capture any network errors or responses
        const responsePromise = page.waitForResponse(
            response => response.url().includes('/api/trades') && response.request().method() === 'POST',
            { timeout: 10000 }
        );

        // Submit the form
        await page.click('button[type="submit"]');

        try {
            // Wait for the API response
            const response = await responsePromise;
            const status = response.status();
            const responseBody = await response.text();

            console.log(`API Response Status: ${status}`);
            console.log(`API Response Body: ${responseBody}`);

            // Check if the response was successful
            if (status === 201 || status === 200) {
                console.log('Trade created successfully!');

                // Parse the response to get trade details
                const trade = JSON.parse(responseBody);
                expect(trade).toHaveProperty('tradeReference');
                expect(trade.baseCurrency).toBe('AUD');
                expect(trade.quoteCurrency).toBe('USD');
                expect(parseFloat(trade.baseAmount)).toBe(421.83);
                expect(parseFloat(trade.exchangeRate)).toBe(0.7);
                expect(trade.counterparty).toBe('JP Morgan');

                console.log(`Trade Reference: ${trade.tradeReference}`);
            } else {
                // Log error details
                console.error('Trade creation failed!');
                console.error(`Status: ${status}`);
                console.error(`Response: ${responseBody}`);

                // Check for error message on the page
                await page.waitForSelector('.alert-danger, .toast-body', { timeout: 2000 }).catch(() => {});
                const errorElement = await page.$('.alert-danger, .toast-body');
                if (errorElement) {
                    const errorText = await page.evaluate(el => el.textContent, errorElement);
                    console.error(`Error message on page: ${errorText}`);
                }

                throw new Error(`Expected successful trade creation but got status ${status}`);
            }
        } catch (error) {
            console.error('Error during trade creation:', error.message);

            // Try to capture any error message displayed on the page
            await page.waitForSelector('.alert-danger, .toast-body', { timeout: 2000 }).catch(() => {});
            const errorElement = await page.$('.alert-danger, .toast-body');
            if (errorElement) {
                const errorText = await page.evaluate(el => el.textContent, errorElement);
                console.error(`Error message on page: ${errorText}`);
            }

            throw error;
        }
    });
});
