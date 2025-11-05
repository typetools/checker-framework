# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

The Checker Framework is a pluggable type-checking system for Java that enables custom compile-time verification of code properties. It consists of multiple type checkers (nullness, signedness, lock, etc.) built on a common framework.

## Build Requirements

- **Java 21+** is required to build the project
- Uses Gradle via the `./gradlew` wrapper script (never use `gradle` directly)
- To specify a different JDK for testing: `./gradlew -PjdkTestVersion=24`

## Common Commands

### Building
```bash
# Build all artifacts (jars and javadoc jars)
./gradlew assemble

# Build only jars needed for javac (faster)
./gradlew assembleForJavac

# Full build with tests
./gradlew build

# Build checker jar with all dependencies
./gradlew checkerJar
```

### Testing
```bash
# Run all tests
./gradlew allTests

# Run tests for a specific checker (e.g., Nullness, Signedness)
./gradlew :checker:NullnessTest
./gradlew :checker:SignednessTest

# Run a specific JUnit test class
cd checker && ../gradlew test --tests '*ParseAllJdkTest'

# Check a single source file with a checker
./gradlew assembleForJavac && checker/bin/javac -processor org.checkerframework.checker.nullness.NullnessChecker -implicit:class checker/tests/nullness/MyFile.java
```

### Style Checking
```bash
# Check Python and shell script style
make style-check

# Auto-fix style issues
make style-fix
```

## Architecture

The repository contains four main subprojects (defined in `settings.gradle`):

1. **javacutil/** - Utilities for integrating with javac compiler
2. **dataflow/** - Dataflow analysis framework (used by Checker Framework, Error Prone, NullAway)
3. **framework/** - The pluggable type-checker framework itself
   - Located at `framework/src/main/java/org/checkerframework/framework/`
   - Provides base classes and infrastructure for building checkers
4. **checker/** - Concrete type checker implementations
   - Located at `checker/src/main/java/org/checkerframework/checker/`
   - Each checker lives in its own package (e.g., `nullness/`, `signedness/`, `lock/`)

Additional subprojects:
- **checker-qual/** - Annotations that checkers recognize
- **checker-util/** - Utility classes for checkers
- **framework-test/** - Testing utilities
- **annotation-file-utilities/** - Tools for working with annotation files

## Checker Implementation Pattern

Each checker typically consists of:
1. `*Checker.java` - Main entry point (extends `BaseTypeChecker`)
2. `*AnnotatedTypeFactory.java` - Type system logic and type hierarchy
3. `*Visitor.java` - AST visitor for custom error checking
4. Annotation definitions in `checker-qual/src/main/java/org/checkerframework/checker/*/qual/`

Tests are located in:
- Test cases: `checker/tests/{checkername}/` (Java files with expected error comments)
- Test runner: `checker/src/test/java/org/checkerframework/checker/test/junit/{CheckerName}Test.java`

## Test File Conventions

Test files in `checker/tests/` use special comment syntax to indicate expected errors:
```java
// :: error: (error.type.identifier)
SomeType x = problematicCode();
```

- Files marked with `// @skip-test` are ignored by the test runner
- Each test file should not issue javac errors and must have unique class names
- Tests are end-to-end compilation tests, not unit tests

## Key Dependencies

The project uses:
- Gradle Shadow plugin for creating fat jars
- Error Prone for additional static analysis
- Spotless for code formatting
- JUnit for testing framework

External libraries are shadowed (packages relocated) in the final checker.jar to avoid conflicts.

## Working with Issues

When addressing GitHub issues:
1. Create a branch named after the issue (e.g., `issue-3211`)
2. Understand the relevant checker by reading its `*Checker.java`, `*AnnotatedTypeFactory.java`, and `*Visitor.java` files
3. Add or enable test cases in `checker/tests/{checkername}/`
4. Run the specific checker test: `./gradlew :checker:{CheckerName}Test`
5. Run full tests before submitting: `./gradlew allTests`

## Developer Resources

- Developer manual: `docs/developer/developer-manual.html`
- Test documentation: `checker/tests/README.md`
- Contributing guide: `CONTRIBUTING.md`
- Changelog: `docs/CHANGELOG.md`
- Main manual: https://checkerframework.org/manual/
