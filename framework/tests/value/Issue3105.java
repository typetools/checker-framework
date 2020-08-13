// Test case for issue #3105: https://tinyurl.com/cfissue/3105

// @skip-test until the bug is fixed

import org.checkerframework.common.value.qual.StringVal;

class Issue3105 {
    public static final String DEMO = "foo";
}

class Demo1 {
    @StringVal("foo") String m() {
        return Issue3105.DEMO;
    }
}

class Demo2 extends Issue3105 {
    @StringVal("foo") String m() {
        return DEMO; // error
    }
}
