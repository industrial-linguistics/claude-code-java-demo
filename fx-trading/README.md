# FX Trading System

A lightweight Foreign Exchange spot trade recording system built with Spring Boot and SQLite.

## Features

- Record spot FX trades with full audit trail
- Track trade lifecycle (PENDING → CONFIRMED → SETTLED)
- REST API for all operations
- SQLite database for simplicity and reliability
- Automated backup scripts
- Minimal operational overhead

## Architecture

- **Framework**: Spring Boot 3.2.1
- **Database**: SQLite with WAL mode
- **Security**: Spring Security with HTTP Basic authentication
- **Schema Management**: Liquibase
- **API**: REST with JSON

## Quick Start

### Prerequisites

- Java 17 or later
- Maven 3.6+
- sqlite3 (for backups)

### Build

```bash
mvn clean package
```

### Run

```bash
./scripts/start.sh
```

The application will start on http://localhost:8080

Default credentials:
- Username: `admin`
- Password: `changeme` (set via `ADMIN_PASSWORD` environment variable)

### Check Status

```bash
./scripts/status.sh
```

### Stop

```bash
./scripts/stop.sh
```

### Backup Database

```bash
./scripts/backup.sh
```

## API Endpoints

### Create Trade

```bash
curl -X POST http://localhost:8080/api/trades \
  -u admin:changeme \
  -H "Content-Type: application/json" \
  -d '{
    "tradeDate": "2025-10-12",
    "valueDate": "2025-10-14",
    "direction": "BUY",
    "baseCurrency": "EUR",
    "quoteCurrency": "USD",
    "baseAmount": 1000000.00,
    "exchangeRate": 1.0850,
    "counterparty": "Bank ABC",
    "trader": "John Doe",
    "notes": "Q1 hedging"
  }'
```

### Get All Trades

```bash
curl -X GET http://localhost:8080/api/trades \
  -u admin:changeme
```

### Get Trade by ID

```bash
curl -X GET http://localhost:8080/api/trades/1 \
  -u admin:changeme
```

### Update Trade

```bash
curl -X PUT http://localhost:8080/api/trades/1 \
  -u admin:changeme \
  -H "Content-Type: application/json" \
  -d '{
    "status": "CONFIRMED",
    "notes": "Updated notes"
  }'
```

### Get Trade Audit History

```bash
curl -X GET http://localhost:8080/api/trades/1/audit \
  -u admin:changeme
```

### Search Trades

By date range:
```bash
curl -X GET "http://localhost:8080/api/trades?startDate=2025-10-01&endDate=2025-10-31" \
  -u admin:changeme
```

By status:
```bash
curl -X GET "http://localhost:8080/api/trades?status=PENDING" \
  -u admin:changeme
```

## Database

The SQLite database is stored at `./data/trades.db` by default.

**Configuration**: WAL mode enabled for better concurrent read performance.

**Backup**: Use `./scripts/backup.sh` - backups are stored in `./backups/` and automatically compressed.

**Restore**: Simply copy a backup file to `./data/trades.db` (after stopping the application).

## Configuration

Edit `src/main/resources/application.yml` to configure:

- Server port
- Database location
- Security settings
- Logging levels
- Validation rules (max amounts, allowed currencies)

## Project Structure

```
fx-trading/
├── src/main/java/com/company/fxtrading/
│   ├── api/              # REST controllers
│   ├── config/           # Spring configuration
│   ├── domain/           # Entity classes
│   ├── repository/       # Data access layer
│   └── service/          # Business logic
├── src/main/resources/
│   ├── application.yml   # Application configuration
│   └── db/changelog/     # Liquibase migrations
├── scripts/              # Operational scripts
│   ├── start.sh
│   ├── stop.sh
│   ├── backup.sh
│   └── status.sh
├── data/                 # SQLite database (created on first run)
├── backups/              # Database backups
└── logs/                 # Application logs
```

## Deployment

For production deployment:

1. Build the JAR: `mvn clean package`
2. Copy to server: `fx-trading-1.0.0.jar` + `application.yml`
3. Create systemd service (see architect's design document)
4. Set up cron job for daily backups
5. Configure `ADMIN_PASSWORD` environment variable

## Maintenance

**Daily Backups**: Add to crontab:
```
0 18 * * * /path/to/fx-trading/scripts/backup.sh
```

**Weekly Optimization**: SQLite VACUUM on Sundays:
```
0 2 * * 0 sqlite3 /path/to/data/trades.db "PRAGMA wal_checkpoint(TRUNCATE); VACUUM;"
```

## Support

For issues or questions, refer to the architectural design document or contact your development team.

## License

Proprietary - Company Internal Use Only
