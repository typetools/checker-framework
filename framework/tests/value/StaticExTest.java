import org.checkerframework.common.value.qual.*;

public class StaticExTest {
    boolean flag;

    void test1() {
        String s = "helloworlod";
        @StringVal({"o", "l"}) String subString = flag ? "o" : "l";
        @IntVal({5, 0, 9}) int start = flag ? 9 : flag ? 5 : 0;
        // flag?1:flag?6:
        @IntVal({-1, 8, 9, 2, 4, 6}) int result = s.indexOf(subString, start);
    }

    void test2() {
        String s = flag ? "helloworlod" : "lololxxolxxxol";
        @StringVal({"o", "l"}) String subString = flag ? "o" : "l";
        @IntVal({0, 9}) int start = flag ? 9 : 0;
        // flag?1:flag?6:
        @IntVal({-1, 0, 1, 2, 4, 9, 12, 13}) int result3 = s.indexOf(subString, start);
    }

    static byte[] b = new byte[0];

    void constructorsArrays() {
        char @ArrayLen(100) [] c = new char[100];
        String s = new String(c);
        new String(b);
    }
}
