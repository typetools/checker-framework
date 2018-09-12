/*
 * @test
 * @summary Test case for Issue 919 https://github.com/typetools/checker-framework/issues/919
 * @compile -processor org.checkerframework.checker.tainting.TaintingChecker ../classes/Issue919.java ../classes/Issue919B.java -AatfCacheSize=9
 */

public class Issue919 {}
