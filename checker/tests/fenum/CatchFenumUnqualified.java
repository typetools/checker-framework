import org.checkerframework.checker.fenum.qual.Fenum;

class CatchFenumUnqualfied {
    void method() {
        try {
        } catch (
                // :: error: (exception.parameter.invalid)
                @Fenum("A") RuntimeException e) {

        }
        try {
            // :: error: (exception.parameter.invalid)
        } catch (@Fenum("A") NullPointerException | @Fenum("A") ArrayIndexOutOfBoundsException e) {

        }
    }
}
