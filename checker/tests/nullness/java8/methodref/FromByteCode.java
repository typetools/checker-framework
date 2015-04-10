

import org.checkerframework.checker.nullness.qual.*;

interface Function<T extends @Nullable Object, R> {
    R apply(T t);
}

class FromByteCode {

    Function<String, String> f1 = String::toString;

    // Make sure there arent any issue's generating an error with a method from byte code
    //:: error: (methodref.param.invalid)
    Function<@Nullable String, String> f2 = String::new;
}