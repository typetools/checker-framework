/*
 * @test
 * @summary Test case for Issue 919 https://github.com/typetools/checker-framework/issues/919
 * @compile -processor org.checkerframework.checker.tainting.TaintingChecker -AprintErrorStack ../classes/Issue919.java ../classes/Issue919B.java -AatfCacheSize=9
 */
import java.util.Set;

public class Issue919 {}
