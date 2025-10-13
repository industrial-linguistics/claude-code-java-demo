# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This repository contains two main components:

1. **FX Trading System** (`fx-trading/`) - A Spring Boot application for recording foreign exchange spot trades with full audit trails
2. **Website** (`website/`) - A landing page for CurrencyPro trading platform (pink Barbie-like theme)

## FX Trading System

### Technology Stack

- **Framework**: Spring Boot 3.2.1
- **Java**: 17+
- **Build Tool**: Maven 3.6+
- **Database**: SQLite with WAL mode
- **ORM**: Hibernate/JPA with SQLite dialect
- **Schema Management**: Liquibase
- **Security**: Spring Security with HTTP Basic authentication
- **Key Libraries**: Lombok, Jackson (JSR310 for dates)

### Architecture

The application follows a layered architecture:

- `api/` - REST controllers exposing JSON endpoints
- `service/` - Business logic and transaction management
- `repository/` - JPA repositories for data access
- `domain/` - JPA entities (Trade, TradeAudit, User)
- `config/` - Spring configuration (Security, Jackson)

### Key Design Patterns

1. **Audit Trail**: Every trade change is recorded in `trade_audit` table with before/after snapshots
2. **Trade Reference Generation**: Format `FX-YYYYMMDD-####` (WARNING: has race condition - see TESTING.md:296)
3. **Optimistic Locking**: Uses `@Version` field to handle concurrent updates
4. **SQLite Concurrency**: Single writer with WAL mode for concurrent reads
5. **BigDecimal**: All financial amounts use BigDecimal (scale=4 for amounts, scale=6 for rates)

### Common Commands

```bash
# Build
cd fx-trading
mvn clean package

# Run application
./scripts/start.sh
# Runs on http://localhost:8080
# Default credentials: admin/changeme

# Check status
./scripts/status.sh

# Stop application
./scripts/stop.sh

# Backup database
./scripts/backup.sh

# Run tests (when implemented)
mvn test
mvn test -Dgroups="unit"        # Fast tests only
mvn test -Dgroups="integration" # Integration tests only
mvn verify                       # Tests + coverage report
```

### Configuration

- **Config file**: `src/main/resources/application.yml`
- **Database path**: Configurable via `FX_DB_PATH` env var (default: `./data/trades.db`)
- **Admin password**: Set via `ADMIN_PASSWORD` env var (default: `changeme`)
- **Max trade amount**: 10,000,000 (configurable in application.yml)
- **Allowed currencies**: EUR,USD,GBP,JPY,CHF,AUD,CAD,NZD,SEK,NOK,DKK

### API Endpoints

All endpoints require HTTP Basic authentication (`admin:changeme` by default):

- `POST /api/trades` - Create new trade
- `GET /api/trades` - List all trades (supports query params: `startDate`, `endDate`, `status`)
- `GET /api/trades/{id}` - Get specific trade
- `PUT /api/trades/{id}` - Update trade (status, notes, counterparty)
- `GET /api/trades/{id}/audit` - Get audit history for trade

### Critical Considerations

1. **Financial Precision**: Always use BigDecimal for amounts and rates. Never use float/double.
2. **Audit Trail**: Every state-changing operation must create an audit record with before/after snapshots.
3. **SQLite Limitations**:
   - Only one writer at a time (writes are serialized)
   - WAL mode enables concurrent reads
   - Connection pool size is 1 (by design)
4. **Race Condition**: Trade reference generation has a known race condition (see TESTING.md:296-331)
5. **Testing**: See `docs/TESTING.md` for comprehensive testing strategy - NO TESTS CURRENTLY EXIST

### Testing Strategy

**CRITICAL**: This is a financial system with no tests currently implemented. See `fx-trading/docs/TESTING.md` for:

- Test-Driven Development approach (Red-Green-Refactor)
- Critical test cases (financial precision, audit trails, SQLite concurrency)
- Known issues to test (trade reference race condition)
- Coverage requirements (80%+ for service/domain layers)
- Test data builders and best practices

**Key principle**: For any feature implementation, write tests first. Financial accuracy is non-negotiable.

### Database Schema

Managed by Liquibase in `src/main/resources/db/changelog/`:

- **trades** - Main trade records with unique trade_reference
- **trade_audit** - Audit trail with JSON snapshots (before/after)
- **users** - User authentication data

### Deployment Notes

- Build produces `fx-trading-1.0.0.jar`
- Requires Java 17+ runtime
- Set `ADMIN_PASSWORD` environment variable in production
- Database backups recommended daily (cron job with `./scripts/backup.sh`)
- Weekly optimization: `PRAGMA wal_checkpoint(TRUNCATE); VACUUM;`

## Website

A static HTML/CSS landing page for CurrencyPro with a **pink Barbie-like theme**.

- `website/index.html` - Main landing page
- `website/styles.css` - Styling with pink/Barbie theme

Open `website/index.html` directly in a browser to view.

## Project-Wide Guidelines

- **Main language**: Java for backend applications
- **Theme**: Website uses pink Barbie aesthetic
- **Code style**: Uses Lombok annotations (@Data, @RequiredArgsConstructor)
- **Date handling**: LocalDate for dates, LocalDateTime for timestamps
- **JSON**: Jackson with JSR310 module for date serialization