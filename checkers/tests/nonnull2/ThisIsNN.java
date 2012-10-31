import checkers.nullness.quals.*;

class ThisIsNN {
    Object out = new Object();

    class Inner {
        void test1() {
            out = this;
            out = ThisIsNN.this;
        }

        Object in = new Object();

        void test2(@Raw Inner this) {
            @SuppressWarnings("rawness") // initialization is now complete
            @NonRaw Object nonRawThis = this;
            out = nonRawThis;
        }

        void test3(@Raw Inner this) {
            @SuppressWarnings("rawness") // initialization is now complete
            @NonRaw Object nonRawThis = ThisIsNN.this;
            out = nonRawThis;
        }
    }

    void test4(@Raw ThisIsNN this) {
        @SuppressWarnings("rawness") // initialization is now complete
        @NonRaw Object nonRawThis = this;
        out = nonRawThis;
    }
}