import testlib.wholeprograminference.qual.*;

public class StringConcatenationTest {

    private String options_str;
    private String options_str2;

    void foo() {
        options_str = getSibling1();
        options_str2 += getSibling1();
    }

    void test() {
        // :: error: (argument.type.incompatible)
        expectsSibling1(options_str);
        // :: error: (argument.type.incompatible)
        expectsSibling1(options_str2);
    }

    void expectsSibling1(@Sibling1 String t) {}

    @Sibling1 String getSibling1() {
        // :: warning: (cast.unsafe)
        return (@Sibling1 String) " ";
    }
}
