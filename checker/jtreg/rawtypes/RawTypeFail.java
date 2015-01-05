/*
 * @test
 * @summary Test that raw types sometimes produces unwanted errors
 *
 * @compile/fail/ref=RawTypeFail.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AprintErrorStack -Alint -Anomsgtext RawTypeFail.java
 * @compile/ref=RawTypeFailIgnored.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AprintErrorStack -Alint -Anomsgtext -AignoreRawTypeArguments RawTypeFail.java
 */

import java.util.Map;
import java.util.HashMap;

class RawTypeFail {
    Map mr = new HashMap();
    Map<String, Object> mc = mr;
    Map<String, Object> mc2 = new HashMap();
}
