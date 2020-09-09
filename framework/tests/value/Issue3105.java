// Test case for issue #3105: https://tinyurl.com/cfissue/3105

import org.checkerframework.common.value.qual.StringVal;

public class Issue3105 {
    public static final String FIELD1 = "foo";

    public static final String FIELD2;

    static {
        FIELD2 = "bar";
    }
}

class Demo1 {
    @StringVal("foo") String m() {
        return Issue3105.FIELD1;
    }
}

class Demo2 extends Issue3105 {
    @StringVal("foo") String m() {
        return FIELD1;
    }
}

class Demo3 {
    @StringVal("bar") String m() {
        return Issue3105.FIELD2;
    }
}

class Demo4 extends Issue3105 {
    @StringVal("bar") String m() {
        return FIELD2;
    }
}
