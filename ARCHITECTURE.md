# Checker Framework Architecture

## Overview

The Checker Framework is a **pluggable type-checking system** for Java that enables developers to create and use custom compile-time type checkers. It operates as a compiler plugin (annotation processor) that extends Java's type system with custom type qualifiers, providing static verification of program properties beyond what the Java compiler normally checks.

The framework supports two primary use cases:
1. **Using built-in checkers** - Apply 20+ production-ready type checkers (Nullness, Lock, Interning, etc.) to detect bugs
2. **Building custom checkers** - Create domain-specific type checkers with minimal code (~100-500 LOC)

---

## System Architecture

```text
┌─────────────────────────────────────────────────────────────────┐
│                        User Application                         │
│              (Java code with type annotations)                  │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                    javac Compiler                               │
│          (with Checker Framework as processor)                  │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                 Checker Framework Modules                       │
│  ┌──────────┐  ┌──────────┐  ┌─────────┐  ┌────────────────┐   │
│  │ checker/ │──│framework/│──│dataflow/│──│   javacutil/   │   │
│  │ (20+     │  │ (Core    │  │(Abstract│  │(javac Wrapper) │   │
│  │Checkers) │  │Framework)│  │Interp.) │  │                │   │
│  └──────────┘  └──────────┘  └─────────┘  └────────────────┘   │
│        │             │             │              │             │
│        └─────────────┴─────────────┴──────────────┘             │
│                          │                                      │
│                          ▼                                      │
│              ┌─────────────────────┐                            │
│              │   checker-qual/     │                            │
│              │   (Annotations)     │                            │
│              └─────────────────────┘                            │
└─────────────────────────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Compilation Result                           │
│         (Type-checked bytecode or error reports)                │
└─────────────────────────────────────────────────────────────────┘
```

---

## Module Description

| Module | Description | Key Components |
|--------|-------------|----------------|
| **`framework/`** | Core type-checking infrastructure enabling pluggable type systems | Type system, flow analysis, stub parsing, visitor framework |
| **`checker/`** | 20+ production-ready type checkers for common bug patterns | Nullness, Lock, Interning, Regex, Units, Tainting, Resource Leak, etc. |
| **`dataflow/`** | Abstract interpretation engine for program analysis | Control flow graph (CFG), forward/backward dataflow, transfer functions |
| **`javacutil/`** | Utilities for working with javac's internal APIs | TreeUtils, ElementUtils, TypesUtils, AnnotationUtils |
| **`checker-qual/`** | Lightweight annotations library (no dependencies) | @NonNull, @Nullable, @Interned, @GuardedBy, @Regex, etc. |
| **`checker-qual-android/`** | Android-compatible qualifier annotations | Subset of checker-qual for Android runtime |
| **`checker-util/`** | Runtime utility methods for checkers | Runtime checks, assertion methods |
| **`annotation-file-utilities/`** | Tools for annotation manipulation | Insert/extract annotations, .jaif file processing |

---

## Detailed Module Breakdown

### 1. `framework/` - Core Type-Checking Framework

The heart of the Checker Framework, providing infrastructure for building custom type checkers.

**Package Structure:**

```text
org/checkerframework/framework/
├── type/                     # Type system infrastructure
│   ├── AnnotatedTypeMirror   # Representation of types with annotations
│   ├── AnnotatedTypeFactory  # Type creation and qualifier inference
│   ├── QualifierHierarchy    # Qualifier subtyping relationships
│   └── GenericAnnotatedTypeFactory  # Base implementation for type factories
├── flow/                     # Flow-sensitive type refinement
│   ├── CFAbstractStore       # Storage of type information at program points
│   ├── CFAbstractAnalysis    # Flow analysis integration
│   ├── CFAbstractTransfer    # Transfer function for dataflow
│   └── CFValue               # Abstract values in dataflow analysis
├── stub/                     # Stub file (.astub) parsing and processing
│   ├── StubParser            # Parser for stub files
│   └── AnnotationFileParser  # Legacy .jaif file support
├── source/                   # Source code processing and visitors
│   ├── SourceChecker         # Base class for all checkers
│   └── SourceVisitor         # AST visitor for type checking
├── util/                     # Framework utilities
│   ├── FlowExpressionParseUtil  # Parse flow expressions
│   ├── dependenttypes/       # Dependent type support
│   └── contract/             # Pre/postcondition processing
└── ajava/                    # Annotation Java (.ajava) file support
```

**Key Concepts:**
- **AnnotatedTypeMirror**: Enhanced version of javac's TypeMirror with type qualifier annotations
- **Type Factories**: Create and cache annotated types, apply default annotations, perform type refinement
- **Qualifier Hierarchies**: Define subtyping relationships between type qualifiers (e.g., @NonNull <: @Nullable)
- **Flow-Sensitive Analysis**: Track how types change through program flow (assignments, conditionals, etc.)
- **Stub Files**: External annotation specifications for libraries without source modifications

### 2. `checker/` - Built-in Type Checkers

Production-ready type checkers for common programming patterns and bug classes.

**Major Checkers:**

```text
org/checkerframework/checker/
├── nullness/              # Null pointer dereference prevention
│   ├── NullnessChecker    # Main checker for @NonNull/@Nullable
│   └── KeyForSubchecker   # Map key existence tracking
├── lock/                  # Thread safety and lock discipline
│   └── LockChecker        # @GuardedBy annotation verification
├── interning/             # Reference equality checking
│   └── InterningChecker   # @Interned for safe == usage
├── regex/                 # Regular expression validation
│   └── RegexChecker       # Compile-time regex syntax checking
├── units/                 # Physical unit verification
│   └── UnitsChecker       # @m, @kg, @s, etc. dimensional analysis
├── tainting/              # Security taint tracking
│   └── TaintingChecker    # @Tainted/@Untainted flow tracking
├── resourceleak/          # Resource management
│   └── ResourceLeakChecker # Must-call and called-methods analysis
├── index/                 # Array bounds checking
│   └── IndexChecker       # @IndexFor, @LTLength annotations
├── signature/             # String format validation
│   └── SignatureChecker   # @ClassGetName, @BinaryName, etc.
├── initialization/        # Object initialization tracking
│   └── InitializationChecker # @Initialized/@UnderInitialization
├── optional/              # Optional misuse detection
│   └── OptionalChecker    # java.util.Optional safety
├── formatter/             # Printf/format string validation
│   └── FormatterChecker   # @Format annotation checking
├── i18n/                  # Internationalization
│   └── I18nChecker        # Localization key validation
├── fenum/                 # Fake enumerations
│   └── FenumChecker       # Type-safe constant groups
├── propkey/               # Property file key validation
├── guieffect/             # UI thread confinement
├── signedness/            # Signed/unsigned integer tracking
└── mustcall/              # Resource lifecycle tracking
```

**Common Structure per Checker:**
- `XxxChecker.java` - Entry point extending `BaseTypeChecker`
- `XxxAnnotatedTypeFactory.java` - Type system and qualifier rules
- `XxxVisitor.java` - AST visitor for custom validation
- `XxxTransfer.java` - Flow-sensitive type refinement logic
- `qual/` subdirectory - Qualifier annotations (@NonNull, @Interned, etc.)
- Stub files - Annotations for JDK and common libraries

### 3. `dataflow/` - Abstract Interpretation Engine

Provides control flow graph construction and dataflow analysis infrastructure.

**Package Structure:**

```text
org/checkerframework/dataflow/
├── cfg/                      # Control Flow Graph
│   ├── ControlFlowGraph      # CFG representation
│   ├── CFGBuilder            # Construct CFG from AST
│   ├── block/                # Basic blocks and special blocks
│   └── node/                 # CFG nodes (assignments, method calls, etc.)
├── analysis/                 # Abstract interpretation framework
│   ├── AbstractAnalysis      # Base class for dataflow analysis
│   ├── ForwardAnalysis       # Forward dataflow (e.g., reaching definitions)
│   ├── BackwardAnalysis      # Backward dataflow (e.g., live variables)
│   ├── TransferFunction      # Transfer functions for dataflow
│   └── Store                 # Abstract state at program points
├── expression/               # Flow expressions (variables, field accesses)
│   └── FlowExpressions       # Represent values tracked in dataflow
├── util/                     # Utilities for dataflow analysis
├── busyexpr/                 # Example: Busy expressions analysis
├── constantpropagation/      # Example: Constant propagation
├── livevariable/             # Example: Live variable analysis
└── reachingdef/              # Example: Reaching definitions
```

**Key Features:**
- **CFG Construction**: Convert Java AST to control flow graph with explicit edges
- **Abstract Interpretation**: Generic framework for forward/backward dataflow analysis
- **Transfer Functions**: Define how abstract values propagate through statements
- **Store Management**: Track abstract values for variables at each program point
- **Example Analyses**: Complete implementations of classic dataflow analyses

### 4. `javacutil/` - javac API Utilities

Wrapper utilities that simplify interaction with javac's complex internal APIs.

**Package Structure:**

```text
org/checkerframework/javacutil/
├── TreeUtils              # Utilities for com.sun.source.tree.* (AST)
├── ElementUtils           # Utilities for javax.lang.model.element.* (symbols)
├── TypesUtils             # Utilities for javax.lang.model.type.* (types)
├── AnnotationUtils        # Annotation processing utilities
├── TreePathCacher         # Cache TreePath objects for performance
├── TypeSystemError        # Error reporting
└── AnnotationProvider     # Access annotations from various sources
```

**Why This Matters:**
- javac's APIs are verbose and low-level
- Utilities provide high-level operations (e.g., "is this a method call?", "get parameter types")
- Consistent null handling and error reporting
- Performance optimizations (caching, memoization)

### 5. `checker-qual/` - Type Qualifier Annotations

Lightweight library containing only annotations, suitable for runtime dependencies.

**Contents:**
- Qualifier annotations (@NonNull, @Nullable, @Interned, @GuardedBy, etc.)
- Meta-annotations (@SubtypeOf, @QualifierForLiterals, @DefaultQualifier, etc.)
- Polymorphic qualifiers (@PolyNull, @PolyInterned, etc.)
- **No implementation code** - zero runtime overhead

**Usage:**
```xml
<dependency>
  <groupId>org.checkerframework</groupId>
  <artifactId>checker-qual</artifactId>
  <version>3.53.0</version>
</dependency>
```

### 6. `annotation-file-utilities/` - Annotation Manipulation Tools

Tools for working with annotations outside source code.

**Components:**
- **Scene-lib**: Library for representing and manipulating annotation scenes
- **JAIF Format**: Java Annotation Index File (.jaif) format for storing annotations
- **Annotation Insertion**: Insert annotations into source code from .jaif files
- **Annotation Extraction**: Extract annotations from class files or source code
- **Command-line Tools**: `insert-annotations`, `extract-annotations`, etc.

**Binaries:**
```
bin/
├── insert-annotations         # Insert from .jaif to source
├── extract-annotations        # Extract from class files
└── insert-annotations-to-source  # Insert maintaining formatting
```

---

## Integration Modes

### 1. Compiler Plugin (Annotation Processor)

**Gradle:**
```gradle
dependencies {
    checkerFramework 'org.checkerframework:checker:3.53.0'
    implementation 'org.checkerframework:checker-qual:3.53.0'
}

tasks.withType(JavaCompile) {
    options.compilerArgs << '-processor' 
    options.compilerArgs << 'org.checkerframework.checker.nullness.NullnessChecker'
}
```

**Maven:**
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <annotationProcessorPaths>
      <path>
        <groupId>org.checkerframework</groupId>
        <artifactId>checker</artifactId>
        <version>3.53.0</version>
      </path>
    </annotationProcessorPaths>
    <compilerArgs>
      <arg>-processor</arg>
      <arg>org.checkerframework.checker.nullness.NullnessChecker</arg>
    </compilerArgs>
  </configuration>
</plugin>
```

**Command Line:**
```bash
javac -processor org.checkerframework.checker.nullness.NullnessChecker MyFile.java
```

### 2. IDE Integration

- **IntelliJ IDEA**: Checker Framework IntelliJ Plugin
- **Eclipse**: Checker Framework Eclipse Plugin
- **VS Code**: Java extension with annotation processor support
- **Real-time Checking**: Errors appear as you type (IDE-dependent)

### 3. Whole-Program Inference (WPI)

Automatically infer type annotations for entire codebases:

```bash
./gradlew wpi
# Or for multi-module projects:
./gradlew wpi-many
```

**Process:**
1. Run type checker with inference enabled
2. Collect constraints from type errors
3. Solve constraints to infer minimal annotations
4. Insert inferred annotations into source code
5. Repeat until fixpoint

### 4. CI/CD Pipeline

```yaml
# GitHub Actions example
- name: Run Checker Framework
  run: ./gradlew build -PcheckerFramework
```

---

## Key Design Patterns

### 1. Visitor Pattern
All type checking uses the Visitor pattern to traverse Java AST:
```java
public class NullnessVisitor extends BaseTypeVisitor<NullnessAnnotatedTypeFactory> {
    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        // Check receiver is @NonNull before calling method
        // ...
        return super.visitMethodInvocation(node, p);
    }
}
```

### 2. Factory Pattern
Type creation centralized in AnnotatedTypeFactory:
```java
AnnotatedTypeMirror type = factory.getAnnotatedType(tree);
```

### 3. Transfer Functions
Dataflow analysis uses transfer functions to model statement effects:
```java
public class NullnessTransfer extends CFAbstractTransfer<...> {
    @Override
    public TransferResult<CFValue, CFStore> visitAssignment(...) {
        // Update store with new type information
    }
}
```

### 4. Qualifier Hierarchy
Type qualifiers organized in subtype lattice:
```
           @Nullable
               |
           @NonNull
```

---

## Extension Points

### Creating a Custom Checker (Minimal Example)

**1. Define Qualifiers:**
```java
@SubtypeOf({}) // Top of hierarchy
public @interface Tainted {}

@SubtypeOf(Tainted.class)
public @interface Untainted {}
```

**2. Create Checker:**
```java
public class TaintingChecker extends BaseTypeChecker {}
```

**3. (Optional) Custom Type Factory:**
```java
public class TaintingAnnotatedTypeFactory 
    extends GenericAnnotatedTypeFactory<...> {
    // Custom inference rules
}
```

**4. (Optional) Custom Visitor:**
```java
public class TaintingVisitor extends BaseTypeVisitor<...> {
    // Custom validation rules
}
```

**Total LOC: ~50-500** depending on complexity. The framework handles:
- Type hierarchy management
- Dataflow analysis
- Subtyping checks
- Default annotation application
- Stub file parsing

---

## Data Flow

### Compilation with Type Checking

```
Source Code (.java)
        ↓
  javac Parser → AST
        ↓
  Checker Framework Processor
        ↓
  ┌──────────────────────┐
  │ 1. Build CFG         │
  │ 2. Run Dataflow      │
  │ 3. Infer Types       │
  │ 4. Check Subtyping   │
  │ 5. Custom Rules      │
  └──────────────────────┘
        ↓
  Type Errors ←─→ Bytecode (.class)
```

### Type Refinement Example

```java
void example(@Nullable String s) {
    if (s != null) {
        // Dataflow refines s to @NonNull here
        int len = s.length(); // OK
    }
    int len2 = s.length(); // ERROR: s is @Nullable
}
```

**Flow-Sensitive Analysis:**
1. Entry: `s` has type `@Nullable String`
2. Condition `s != null`: Creates two branches
3. True branch: Refine `s` to `@NonNull String`
4. False branch: Keep `s` as `@Nullable String`
5. After if: Merge types → `@Nullable String`

---

## Performance Considerations

### Compilation Time Impact
- **Typical overhead**: 10-50% increase in compilation time
- **Factors**: Number of checkers, codebase size, annotation density
- **Optimization**: Use incremental compilation, cache CFG/dataflow results

### Scaling Strategies
1. **Incremental checking**: Only check changed files
2. **Parallel checking**: Run multiple checkers in parallel (if independent)
3. **Selective checking**: Check only critical modules in CI
4. **Inference caching**: Cache whole-program inference results

---

## Testing Infrastructure

### Test Framework
```
*/tests/
├── *.java          # Test input files with type annotations
├── *.out           # Expected javac output (errors/warnings)
└── build.gradle    # Test execution configuration
```

### Running Tests
```bash
./gradlew :checker:test              # All checker tests
./gradlew :framework:test            # Framework tests
./gradlew :dataflow:test             # Dataflow tests
./gradlew test                       # All tests
```

### Test Categories
- **Unit tests**: Individual components (type factories, utilities)
- **Integration tests**: Full checker runs on sample code
- **Regression tests**: Previously fixed bugs
- **JDK tests**: Verify checker works with all JDK versions

---

## Documentation

| Resource | Location |
|----------|----------|
| **User Manual** | `docs/manual/` (also at checkerframework.org/manual/) |
| **Developer Guide** | `docs/developer/` |
| **API Javadoc** | Generated during build (`./gradlew javadoc`) |
| **Tutorial** | `docs/tutorial/` |
| **Examples** | `docs/examples/` |
| **Changelog** | `docs/CHANGELOG.md` |

---

## Build System

**Technology**: Gradle 8+ with Kotlin DSL support

**Key Gradle Tasks:**
```bash
./gradlew build              # Build all modules
./gradlew checker:build      # Build checker module
./gradlew test               # Run all tests
./gradlew javadoc            # Generate API docs
./gradlew assemble           # Build JARs without tests
./gradlew shadowJar          # Build fat JAR with dependencies
```

**Artifact Publishing:**
- Maven Central: `org.checkerframework:checker`, `checker-qual`, etc.
- GitHub Releases: Full distribution with tools and docs

---

## Version Support

| Component | Version Support |
|-----------|----------------|
| **Java Language** | Java 8, 11, 17, 21, 22+ (latest LTS + current) |
| **Build Tool** | Gradle 7+, Maven 3.6+, Ant 1.10+ |
| **IDE** | IntelliJ IDEA 2020+, Eclipse 2020+, VS Code (latest) |
| **JDK Build** | Requires JDK 21+ to build the framework itself |

---

## Security & Quality

### Static Analysis on Checker Framework Itself
The framework uses its own checkers during development:
- Nullness Checker: Prevent null pointer exceptions
- Lock Checker: Ensure thread safety
- Initialization Checker: Prevent uninitialized reads

### Code Quality Tools
- **Spotless**: Code formatting (Google Java Style)
- **Error Prone**: Additional static analysis
- **JaCoCo**: Code coverage measurement
- **CheckStyle**: Style checking

---

## Community & Contributing

- **License**: GPL2 with Classpath Exception (mostly), some components MIT
- **Contributors**: 110+ developers, thousands of users
- **Issue Tracker**: GitHub Issues
- **Mailing List**: checker-framework-discuss@googlegroups.com
- **Contributing Guide**: `CONTRIBUTING.md`

---

## References

- **Website**: [checkerframework.org](https://checkerframework.org)
- **GitHub**: [typetools/checker-framework](https://github.com/typetools/checker-framework)
- **Manual**: [checkerframework.org/manual](https://checkerframework.org/manual/)
- **Developer Manual**: [developer manual](https://checkerframework.org/manual/developer-manual.html)
- **Academic Papers**: [checkerframework.org/papers](https://checkerframework.org/papers/)

---

## Glossary

- **Annotation Processor**: Java compiler plugin that runs during compilation
- **Annotated Type**: Type with additional qualifier annotations (e.g., `@NonNull String`)
- **CFG**: Control Flow Graph - directed graph representing program execution paths
- **Dataflow Analysis**: Computing facts about program values at each program point
- **Qualifier**: Type annotation that refines a type (e.g., @NonNull, @Interned)
- **Stub File**: External annotation specification for libraries (.astub format)
- **Type Factory**: Component that creates and manages annotated types
- **Type Hierarchy**: Subtyping relationships between qualified types
- **Transfer Function**: Function modeling how statements affect abstract values
- **WPI**: Whole-Program Inference - automatic annotation inference

---

*This architecture document describes the Checker Framework as of version 3.53+. For the latest information, see the [official documentation](https://checkerframework.org/manual/).*
