// Test case for issue #3105: https://tinyurl.com/cfissue/3105

// @skip-test It passes, but requires the Issue3105B class to be pre-compiled
// and I can't seem to get that class on the proper path when using jtreg.

import org.checkerframework.common.value.qual.StringVal;

public class Issue3105B {
    public static final String FIELD1 = "foo";

    public static final String FIELD2;

    static {
        FIELD2 = "bar";
    }
}

class Demo1 {
    @StringVal("foo") String m() {
        return Issue3105B.FIELD1;
    }
}

class Demo2 extends Issue3105B {
    @StringVal("foo") String m() {
        return FIELD1;
    }
}

class Demo3 {
    @StringVal("bar") String m() {
        return Issue3105B.FIELD2;
    }
}

class Demo4 extends Issue3105B {
    @StringVal("bar") String m() {
        return FIELD2;
    }
}
