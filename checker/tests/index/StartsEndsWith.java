// Tests string length refinement after startsWith or endsWith return true
// https://github.com/kelloggm/checker-framework/issues/56

import org.checkerframework.common.value.qual.MinLen;

class StartsEndsWith {

    // This particular test is here rather than in the framework tests because it depends on purity
    // annotations for these particular JDK methods.
    void refineStartsConditional(String str, String prefix) {
        if (prefix.length() > 10 && str.startsWith(prefix)) {
            @MinLen(11) String s11 = str;
        }
    }
}

class StartsEndsWithExternal {
    public static final String staticFinalField = "str";
}
