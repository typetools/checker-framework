/*
 * @test
 * @summary Test problem with aliasing LengthOf -> NonNegative and NonNegative -> IntRangeFromNonNegative
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.index.IndexChecker -AprintErrorStack valuestub/Test.java -Astubs=valuestub/Test.astub
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.index.IndexChecker -AprintErrorStack valuestub/UseTest.java
 */

class ValueStubDriver {}
