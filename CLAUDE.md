# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a TDD-based Java Spring Boot application for managing user points. The project is in early development stages (step1 branch) with a focus on implementing point charging, usage, and history tracking functionality.

## Build System

This project uses **Gradle** with Kotlin DSL:
- Java 17
- Spring Boot
- Lombok for boilerplate reduction
- JaCoCo for code coverage

## Common Commands

### Building and Running
```bash
./gradlew build              # Build the project
./gradlew bootRun           # Run the Spring Boot application
./gradlew clean build       # Clean and build from scratch
```

### Testing
```bash
./gradlew test              # Run all tests
./gradlew test --tests PointServiceTest  # Run a specific test class
./gradlew test --tests PointServiceTest.testMethodName  # Run a specific test method
./gradlew jacocoTestReport  # Generate test coverage report (outputs to build/reports/jacoco/test/html)
```

**Note**: Tests are configured with `ignoreFailures = true` in build.gradle.kts, meaning the build will continue even if tests fail.

### Code Quality
```bash
./gradlew check             # Run all checks including tests
./gradlew jacocoTestCoverageVerification  # Verify coverage thresholds
```

## Architecture

### Layer Structure
The application follows a typical Spring Boot three-tier architecture:

1. **Controller Layer** (`io.hhplus.tdd.point.PointController`)
   - REST endpoints for point operations
   - Uses `@RestController` with `/point` base path
   - Delegates to service layer

2. **Service Layer** (`io.hhplus.tdd.point.PointService`)
   - Business logic for point operations
   - Currently contains basic validation (user ID must be > 0)
   - Main methods: `getUserPoint()`, `getPointHistory()`, `charge()`, `use()`

3. **Database Layer** (`io.hhplus.tdd.database.*`)
   - **Important**: Table classes (`UserPointTable`, `PointHistoryTable`) should NOT be modified
   - Only use the public API methods provided by these classes
   - Tables simulate database latency with random sleep (200-300ms)
   - In-memory storage using `HashMap` (UserPointTable) and `ArrayList` (PointHistoryTable)

### Domain Models
- `UserPoint`: Record class representing user point data (id, point, updateMillis)
- `PointHistory`: Record class for transaction history (id, userId, amount, type, updateMillis)
- `TransactionType`: Enum for CHARGE/USE operations

### Global Error Handling
- `ApiControllerAdvice`: Global exception handler using `@RestControllerAdvice`
- Returns 500 status with standardized `ErrorResponse` for all exceptions

## Development Notes

### Database Table Classes
The `UserPointTable` and `PointHistoryTable` classes are provided as-is and should be treated as external dependencies. Do not modify these classes; only use their public APIs:
- `UserPointTable.selectById(Long id)`: Retrieves user point or empty record
- `UserPointTable.insertOrUpdate(long id, long amount)`: Updates user points
- `PointHistoryTable.insert(long userId, long amount, TransactionType type, long updateMillis)`: Records transaction
- `PointHistoryTable.selectAllByUserId(long userId)`: Retrieves user's transaction history

### Current Implementation Status
- Basic validation logic exists in `PointService` (user ID > 0 check)
- Service methods return `null` or empty collections (marked with TODOs)
- Test class `PointServiceTest` is empty and needs implementation
- This is a TDD project - write tests first, then implement functionality
