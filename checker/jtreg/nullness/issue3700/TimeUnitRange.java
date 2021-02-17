/*
 * @test
 * @summary Test case for Issue #3700 https://github.com/typetools/checker-framework/issues/3700
 * @compile -XDrawDiagnostics -Xlint:unchecked TimeUnitRange.java
 * @compile/ref=Client.out -processor org.checkerframework.checker.nullness.NullnessChecker Client.java -Astubs=TimeUnitRange.astub -implicit:none -Anomsgtext
 */

public enum TimeUnitRange {
    YEAR,
    YEAR_TO_MONTH,
    MONTH;

    public static TimeUnitRange of(Object endUnit) {
        throw new Error("body is irrelevant");
    }
}
