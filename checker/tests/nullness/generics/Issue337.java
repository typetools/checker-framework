// Test case for Issue 337:
// https://code.google.com/p/checker-framework/issues/detail?id=337

import javax.annotation.Nullable;

abstract class Test<R> {
    abstract R getThing(String key);

    @Nullable R m1(@Nullable String key) {
        // TODO this is unexpected
        //:: error: (conditional.type.incompatible)
        return (key == null) ? null : getThing(key);
    }

    @Nullable R m2(@Nullable String key) {
        return (key == null) ?
            //:: error: (argument.type.incompatible)
            getThing(key) :
            null;
    }

}
