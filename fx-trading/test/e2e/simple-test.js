/**
 * Simple Puppeteer Test - Verify Trade Loading Works
 *
 * This standalone test verifies that the FX Trading UI loads
 * without showing "Failed to load trades" error.
 */

const puppeteer = require('puppeteer');

const BASE_URL = process.env.TEST_URL || 'http://localhost:8080';

(async () => {
    console.log('Starting FX Trading UI Test...\n');

    const browser = await puppeteer.launch({
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });

    const page = await browser.newPage();

    // Track errors
    let hasError = false;
    let errorMessage = '';

    page.on('console', msg => {
        const text = msg.text();
        if (msg.type() === 'error') {
            console.log(`[Browser Error]: ${text}`);
            if (text.includes('Failed to load trades') || text.includes('Error loading trades')) {
                hasError = true;
                errorMessage = text;
            }
        }
    });

    page.on('pageerror', error => {
        console.error('[Page Error]:', error.message);
    });

    try {
        console.log(`Navigating to ${BASE_URL}...`);
        await page.goto(BASE_URL, { waitUntil: 'networkidle2', timeout: 10000 });

        console.log('✓ Page loaded successfully');

        // Check page title
        const title = await page.title();
        console.log(`✓ Page title: "${title}"`);

        if (title !== 'FX Trading System') {
            throw new Error(`Expected title "FX Trading System", got "${title}"`);
        }

        // Wait for the trades table to be present
        console.log('Waiting for trades table...');
        await page.waitForSelector('#tradesTableBody', { timeout: 5000 });
        console.log('✓ Trades table found');

        // Wait a moment for any API calls to complete
        await new Promise(resolve => setTimeout(resolve, 2000));

        // Check for error toast
        const toastText = await page.evaluate(() => {
            const toast = document.getElementById('toastMessage');
            return toast ? toast.textContent : null;
        });

        if (toastText) {
            console.log(`Toast message present: "${toastText}"`);
            if (toastText.includes('Failed to load trades') || toastText.includes('Error loading trades')) {
                throw new Error(`Found error message in toast: ${toastText}`);
            }
        } else {
            console.log('✓ No error toast displayed');
        }

        // Check table content
        const tableContent = await page.$eval('#tradesTableBody', el => el.textContent);

        if (!tableContent || tableContent.trim().length === 0) {
            throw new Error('Trades table is empty (no content at all)');
        }

        console.log('✓ Trades table has content');

        // Verify it's either showing "No trades found" or actual trades
        if (tableContent.includes('No trades found')) {
            console.log('✓ Showing "No trades found" message (database is empty)');
        } else if (tableContent.includes('FX-')) {
            console.log('✓ Showing trade data');
        } else {
            console.log(`Warning: Unexpected table content: "${tableContent.substring(0, 100)}..."`);
        }

        // Final check for any error that was logged
        if (hasError) {
            throw new Error(`JavaScript error detected: ${errorMessage}`);
        }

        console.log('\n✅ TEST PASSED: No "Failed to load trades" error!\n');
        await browser.close();
        process.exit(0);

    } catch (error) {
        console.error('\n❌ TEST FAILED:', error.message, '\n');
        await browser.close();
        process.exit(1);
    }
})();
