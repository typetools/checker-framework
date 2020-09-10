// Test case for issue #3105: https://tinyurl.com/cfissue/3105

import org.checkerframework.common.value.qual.StringVal;

public class Issue3105 {
    public static final String FIELD1 = "foo";
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
