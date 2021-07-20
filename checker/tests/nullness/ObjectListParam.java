import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

import java.util.List;

class ObjectListParam {
    // :: error: type.argument.type.incompatible
    void test(List<@UnknownInitialization Object> args) {
        for (Object obj : args) {
            boolean isClass = obj instanceof Class<?>;
            // :: error: initialization.invalid.cast
            @Initialized Class<?> clazz = (isClass ? (@Initialized Class<?>) obj : obj.getClass());
        }
    }
}
