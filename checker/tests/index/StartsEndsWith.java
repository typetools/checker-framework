// Tests string length refinement after startsWith or endsWith return true
// https://github.com/kelloggm/checker-framework/issues/56

import org.checkerframework.common.value.qual.MinLen;

public class StartsEndsWith {

    final String prefix;

    StartsEndsWith(String prefix) {
        this.prefix = prefix;
    }

    String propertyName(String methodName) {
        if (methodName.startsWith(prefix)) {
            @SuppressWarnings(
                    "index") // BUG: https://github.com/typetools/checker-framework/issues/5201
            String result = methodName.substring(prefix.length());
            return result;
        } else {
            return null;
        }
    }

    // This particular test is here rather than in the framework tests because it depends on purity
    // annotations for these particular JDK methods.
    static void refineStartsConditional(String str, String prefix) {
        if (prefix.length() > 10 && str.startsWith(prefix)) {
            @MinLen(11) String s11 = str;
        }
    }
}

class StartsEndsWithExternal {
    public static final String staticFinalField = "str";
}
