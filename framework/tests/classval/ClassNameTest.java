import org.checkerframework.common.reflection.qual.ClassVal;

public class ClassNameTest {
    void test() throws Exception {
        @ClassVal("Class$Inner") Object o;
        @ClassVal("java.lang.String") Object o1;
        @ClassVal("java.lang.String[]") Object o2;
        @ClassVal("java.lang.String[][][]") Object o3;
        @ClassVal("Class$Inner._") Object o8;

        // :: error: (illegal.classname)
        @ClassVal("java.lang.String[][]]") Object o4;
        // :: error: (illegal.classname)
        @ClassVal("java.lang.String[][][") Object o5;
        // :: error: (illegal.classname)
        @ClassVal("java.lang.String[][][]s") Object o6;
        // :: error: (illegal.classname)
        @ClassVal("java.lang.String[][][].") Object o7;
        // :: error: (illegal.classname)
        @ClassVal("java.lang..String") Object o9;
    }
}
