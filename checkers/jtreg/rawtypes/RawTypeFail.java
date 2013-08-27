/*
 * @test
 * @summary Test that raw types sometimes produces unwanted errors
 *
 * @compile/ref=RawTypeFail.out -XDrawDiagnostics -Xlint:unchecked -processor checkers.nullness.NullnessChecker -AprintErrorStack -Alint -Anomsgtext RawTypeFail.java
 * @compile/ref=RawTypeFailIgnored.out -XDrawDiagnostics -Xlint:unchecked -processor checkers.nullness.NullnessChecker -AprintErrorStack -Alint -Anomsgtext -AignoreRawTypeArguments RawTypeFail.java
 */

import java.util.Map;
import java.util.HashMap;

class RawTypeFail {
    Map mr = new HashMap();
    Map<String, Object> mc = mr;
}
