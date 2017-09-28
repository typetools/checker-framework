/*
 * @test
 * @summary Test problem with aliasing LengthOf -> NonNegative and NonNegative -> IntRangeFromNonNegative
 *
 * @compile -XDrawDiagnostics NeedsIntRange.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.common.value.ValueChecker UsesIntRange.java -Astubs=NeedsIntRange.astub
 */

class Main {}
