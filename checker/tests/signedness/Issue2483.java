import org.checkerframework.checker.signedness.qual.*;

class Issue2483 {
    void foo(String a, byte[] b) {
        @Unsigned int len = a.length();
        @Unsigned int len2 = b.length;
    }
}
