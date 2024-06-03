/*
 * @test
 * @summary Test problem with aliasing LengthOf -> NonNegative and NonNegative -> IntRangeFromNonNegative
 *
 * @compile -processor org.checkerframework.checker.index.IndexChecker valuestub/Test.java -Astubs=valuestub/Test.astub
 * @compile -processor org.checkerframework.checker.index.IndexChecker valuestub/UseTest.java
 */

public class ValueStubDriver {}
