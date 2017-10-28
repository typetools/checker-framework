import org.checkerframework.checker.index.qual.NonNegative;

public class InvalidatedAnnotationOnInnerClass {

    InnerClass myClass;

    void do_things(@NonNegative int x) {
        if (x < myClass.innerArray.length) {
            int y = myClass.innerArray[x];
            havoc();
            //:: error: (array.access.unsafe.high.range)
            int z = myClass.innerArray[x];
        }
    }

    void havoc() {
        myClass.innerArray = null;
    }

    class InnerClass {
        public int[] innerArray;
    }
}
