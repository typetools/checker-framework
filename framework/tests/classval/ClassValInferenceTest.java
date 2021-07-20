import org.checkerframework.common.reflection.qual.ClassBound;
import org.checkerframework.common.reflection.qual.ClassVal;

import java.util.ArrayList;
import java.util.List;

public class ClassValInferenceTest {

    class Inner {
        Inner() {
            @ClassBound("ClassValInferenceTest$Inner") Class<?> c1 = this.getClass();
            @ClassBound("ClassValInferenceTest") Class<?> c2 = ClassValInferenceTest.this.getClass();
        }
    }

    public void classLiterals() {
        @ClassVal("java.lang.Object") Class<?> c1 = Object.class;
        @ClassVal("java.lang.Object[]") Class<?> c2 = Object[].class;
        @ClassVal("java.lang.Object[][][]") Class<?> c3 = Object[][][].class;
        @ClassVal("ClassValInferenceTest$Inner") Class<?> c4 = Inner.class;
        @ClassVal("byte") Class<? extends Byte> c5 = byte.class;
    }

    public void classForName() throws ClassNotFoundException {
        @ClassVal("ClassValInferenceTest$Inner") Class<?> c = Class.forName("ClassValInferenceTest$Inner");
        @ClassVal("java.lang.Object") Class<?> c1 = Class.forName("java.lang.Object");
    }

    boolean flag = true;

    public void classForNameStringVal() throws ClassNotFoundException {
        Class<?> c2;
        if (flag) {
            c2 = Class.forName("java.lang.Byte");
        } else {
            c2 = Class.forName("java.lang.Integer");
        }
        @ClassVal({"java.lang.Byte", "java.lang.Integer"}) Class<?> c3 = c2;
    }

    public <T extends Number, I extends Number & List<String>> void testGetClass(
            T typeVar, I intersect) {
        @ClassBound("ClassValInferenceTest") Class<?> c1 = this.getClass();
        @ClassBound("ClassValInferenceTest") Class<?> c2 = getClass();
        String[] array = {"hello"};
        @ClassBound("java.lang.String[]") Class<?> c3 = array.getClass();
        String[][][][] arrayMulti = null;
        @ClassBound("java.lang.String[][][][]") Class<?> c4 = arrayMulti.getClass();
        @ClassBound("java.lang.String") Class<?> c5 = array[0].getClass();
        List<String> list = null;
        @ClassBound("java.util.List") Class<?> c6 = list.getClass();
        @ClassBound("java.lang.Number") Class<?> c7 = typeVar.getClass();
        @ClassBound("java.util.ArrayList") Class<?> c8 = new ArrayList<String>().getClass();
        List<? super Number> wildCardListLB = null;
        List<? extends Number> wildCardListUB = null;
        @ClassBound("java.lang.Object") Class<?> c9 = wildCardListLB.get(0).getClass();
        @ClassBound("java.lang.Number") Class<?> c10 = wildCardListUB.get(0).getClass();
        Integer i = 0;
        @ClassBound("java.lang.Integer") Class<?> c11 = i.getClass();
        @ClassBound("java.lang.Object") Class<?> c12 = intersect.getClass();

        try {
        } catch (NullPointerException | ArrayIndexOutOfBoundsException ex) {
            @ClassBound("java.lang.RuntimeException") Class<?> c = ex.getClass();
        }
    }
}
