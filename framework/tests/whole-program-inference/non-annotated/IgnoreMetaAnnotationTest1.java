import tests.wholeprograminference.qual.Sibling1;

// See ExpectedErrors#IgnoreMetaAnnotationTest2
class IgnoreMetaAnnotationTest1 {

    int field2;
    void foo() {
        field2 = getSibling1();
    }

    void test() {
        //:: error: (argument.type.incompatible)
        expectsSibling1(field2);
    }

    void expectsSibling1(@Sibling1 int t) {}
    static @Sibling1 int getSibling1() {
        return 0;
    }
}
