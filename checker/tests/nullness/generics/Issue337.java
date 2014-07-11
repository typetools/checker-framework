// Test case for Issue 337:
// https://code.google.com/p/checker-framework/issues/detail?id=337

import javax.annotation.Nullable;

abstract class Test<R> {
    abstract R getThing(String key);

    @Nullable R m1(@Nullable String key) {
        return (key == null) ?
            null :
            getThing(key);
    }

    @Nullable R m1b(@Nullable String key) {
        return (key != null) ?
            getThing(key) :
            null;
    }

    @Nullable R m2(@Nullable String key) {
        return (key == null) ?
            //:: error: (argument.type.incompatible)
            getThing(key) :
            null;
    }

    @Nullable R m2b(@Nullable String key) {
        return (key != null) ?
            null :
            //:: error: (argument.type.incompatible)
            getThing(key);
    }

}
