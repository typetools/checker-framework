import org.checkerframework.checker.index.qual.*;

class SameLenWithObjects {

    class SimpleCollection {
        Object[] var_infos;
    }

    static final class Invocation1 {
        SimpleCollection sc;
        Object @SameLen({"vals1", "this.sc.var_infos"}) [] vals1;

        void format1() {
            for (int j = 0; j < vals1.length; j++) {
                System.out.println(sc.var_infos[j]);
            }
        }
    }
}
