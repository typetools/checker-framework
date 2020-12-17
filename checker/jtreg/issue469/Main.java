/*
 * @test
 * @summary Test that Issue 469 is fixed. Thanks to user pSub for the test case.
 * @library .
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.regex.RegexChecker simplecrash/CrashyInterface.java simplecrash/LetItCrash.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.regex.RegexChecker simplecrash/LetItCrash.java simplecrash/CrashyInterface.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.regex.RegexChecker simplecrash/CrashyInterface.java simplecrash/LetItCrash.java simplecrash/SomeRandomClass.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.regex.RegexChecker simplecrash/LetItCrash.java simplecrash/CrashyInterface.java simplecrash/SomeRandomClass.java
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker advancedcrash/CrashyInterface.java advancedcrash/LetItCrash.java advancedcrash/SomeInterface.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker advancedcrash/LetItCrash.java advancedcrash/CrashyInterface.java advancedcrash/SomeInterface.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker advancedcrash/LetItCrash.java advancedcrash/SomeInterface.java  advancedcrash/CrashyInterface.java
 */
public class Main {}
