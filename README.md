# Programming with Claude Code - Demo Repository

This repository contains demo projects for the **Programming with Claude Code** course, showcasing practical examples of AI-assisted software development using Claude Code.

## Projects

### 1. FX Trading System (`fx-trading/`)

A production-ready Foreign Exchange spot trade recording system built with Spring Boot and SQLite, demonstrating:

- **Full-stack development**: REST API backend + responsive web UI
- **Database design**: SQLite with Liquibase migrations and full audit trails
- **Financial accuracy**: BigDecimal precision for all monetary calculations
- **Concurrency handling**: Optimistic locking with JPA `@Version`
- **Testing**: Puppeteer end-to-end tests for UI workflows
- **Security**: Spring Security with HTTP Basic authentication

#### Key Features

- Record FX spot trades with complete audit history
- Support for 11 major currencies (EUR, USD, GBP, JPY, CHF, AUD, CAD, NZD, SEK, NOK, DKK)
- Trade lifecycle management (PENDING → CONFIRMED → SETTLED)
- Unique trade reference generation (format: `FX-YYYYMMDD-####`)
- Real-time trade search and filtering
- RESTful JSON API with full CRUD operations

#### Technology Stack

- **Backend**: Java 17, Spring Boot 3.2.1, Hibernate/JPA
- **Database**: SQLite 3.x with WAL mode
- **Frontend**: HTML5, CSS3, Vanilla JavaScript, Bootstrap 5
- **Testing**: JUnit 5, Jest, Puppeteer
- **Build**: Maven 3.6+

#### Quick Start

```bash
cd fx-trading

# Build the application
mvn clean package

# Start the server
./scripts/start.sh

# Check status
./scripts/status.sh

# Stop the server
./scripts/stop.sh
```

The application runs at http://localhost:8080 (default credentials: `admin:changeme`)

#### API Endpoints

All endpoints require HTTP Basic authentication:

- `POST /api/trades` - Create new trade
- `GET /api/trades` - List all trades (supports filtering)
- `GET /api/trades/{id}` - Get specific trade
- `PUT /api/trades/{id}` - Update trade
- `GET /api/trades/{id}/audit` - Get audit history

#### Running Tests

```bash
# Run all tests
mvn test

# Run Puppeteer UI tests
npm test

# Run specific test
npm test -- test/e2e/create-aud-usd-trade.test.js
```

#### Architecture Highlights

**Layered Architecture**:
- `api/` - REST controllers
- `service/` - Business logic and transactions
- `repository/` - JPA data access
- `domain/` - JPA entities with audit support
- `config/` - Spring configuration and type converters

**SQLite Type Converters**:
Custom JPA `AttributeConverter` implementations handle LocalDate and LocalDateTime storage in SQLite's TEXT columns, ensuring proper ISO-8601 formatting with backward compatibility for epoch milliseconds.

**Audit Trail Pattern**:
Every state change creates an immutable audit record with before/after JSON snapshots, providing complete trade history for compliance and debugging.

#### Known Issues

- Trade reference generation has a race condition under high concurrency (see `docs/TESTING.md:296-331`)
- SQLite supports only one writer at a time (writes are serialized)

### 2. CurrencyPro Website (`website/`)

A static landing page for the CurrencyPro trading platform featuring a pink Barbie-inspired theme. Demonstrates rapid prototyping of marketing sites with AI assistance.

## Course Learning Objectives

This repository demonstrates key concepts taught in the **Programming with Claude Code** course:

1. **AI-Assisted Development**
   - Rapid prototyping and iteration
   - Code generation with context awareness
   - Debugging and issue diagnosis with AI

2. **Production Code Quality**
   - Financial-grade precision and accuracy
   - Comprehensive error handling
   - Security best practices

3. **Testing Strategy**
   - Unit, integration, and E2E testing
   - Browser automation with Puppeteer
   - Test-driven development workflows

4. **Database Design**
   - Schema migrations with Liquibase
   - Audit trail implementation
   - SQLite optimization for production use

5. **Full-Stack Integration**
   - RESTful API design
   - Modern web UI patterns
   - Authentication and authorization

## Development Workflow

This project was developed entirely using Claude Code, demonstrating:

- Iterative refinement of requirements
- Real-time debugging and problem-solving
- Automated test creation and execution
- Documentation generation
- Git workflow management

## Configuration

### Environment Variables

- `FX_DB_PATH` - Database file location (default: `./data/trades.db`)
- `ADMIN_PASSWORD` - Admin user password (default: `changeme`)

### Database Maintenance

```bash
# Backup database
./scripts/backup.sh

# Optimize database (run weekly)
sqlite3 data/trades.db "PRAGMA wal_checkpoint(TRUNCATE); VACUUM;"
```

## Documentation

- [Testing Strategy](fx-trading/docs/TESTING.md) - Comprehensive testing guide
- [API Documentation](fx-trading/docs/API.md) - REST endpoint reference (if exists)
- [Architecture Overview](fx-trading/docs/ARCHITECTURE.md) - System design details (if exists)

## Contributing

This is a demo repository for educational purposes. For the Programming with Claude Code course, students are encouraged to:

1. Fork this repository
2. Experiment with new features
3. Try different AI prompting strategies
4. Compare human-written vs AI-assisted code

## License

ISC License - Free for educational use.

## Course Information

**Programming with Claude Code**
Learn modern software development with AI assistance, covering full-stack web applications, testing strategies, and production deployment patterns.

---

*This repository and its contents were developed using Claude Code to demonstrate AI-assisted software development workflows.*
