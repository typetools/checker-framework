// Test case for issue #3105: https://tinyurl.com/cfissue/3105
// Issue3105B is framework/src/test/java/testlib/lib/Issue3105B.java.
import org.checkerframework.common.value.qual.StringVal;
import testlib.lib.Issue3105B;

class Issue3105C {
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
}
