/*
 * @test
 * @summary Test that raw types sometimes produces unwanted errors
 * @ignore Renable once Issue #635 is fixed (https://github.com/typetools/checker-framework/issues/635)
 *
 * @compile/fail/ref=RawTypeFail.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Alint -Anomsgtext RawTypeFail.java
 * @compile/ref=RawTypeFailIgnored.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Alint -Anomsgtext -AignoreRawTypeArguments RawTypeFail.java
 */

import java.util.HashMap;
import java.util.Map;

public class RawTypeFail {
    Map mr = new HashMap();
    Map<String, Object> mc = mr;
    Map<String, Object> mc2 = new HashMap();
}
