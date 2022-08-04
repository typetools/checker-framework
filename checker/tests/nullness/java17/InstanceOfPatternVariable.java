// @below-java17-jdk-skip-test
// Test case for https://github.com/typetools/checker-framework/issues/5240

import org.checkerframework.checker.nullness.qual.KeyFor;

import java.util.List;
import java.util.Map;

public class InstanceOfPatternVariable {

    public void doSomething(final Type type) {
        if (type instanceof ClassOrInterfaceType ct) {
            //final var ct = (ClassOrInterfaceType) type;

          List<Map.Entry<@KeyFor("ct.getFields()") String, Field>> finalFields = null;
        }
    }
}
