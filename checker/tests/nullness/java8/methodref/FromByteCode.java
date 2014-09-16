

import org.checkerframework.checker.nullness.qual.*;

import tests.util.function.*;

class FromByteCode {

    Function<String, String> f1 = String::toString;

    // Make sure there arent any issue's generating an error with a method from byte code
    //:: error: (methodref.param.invalid)
    Function<@Nullable String, String> f2 = String::new;
}