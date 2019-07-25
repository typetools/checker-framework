import org.checkerframework.checker.nullness.qual.*;

class ThisIsNN {
    Object out = new Object();

    class Inner {
        void test1() {
            out = this;
            out = ThisIsNN.this;
        }

        Object in = new Object();

        void test2(Inner this) {
            Object nonRawThis = this;
            out = nonRawThis;
        }

        void test3(Inner this) {
            Object nonRawThis = ThisIsNN.this;
            out = nonRawThis;
        }
    }

    void test4(ThisIsNN this) {
        Object nonRawThis = this;
        out = nonRawThis;
    }
}
