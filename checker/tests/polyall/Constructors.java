import org.checkerframework.checker.polyall.quals.*;

class Constructors {
    @H1S2 @H2S2 Constructors() {}

    void test1() {
        // All quals from constructor
        @H1S2 @H2S2 Constructors c1 = new Constructors();
    }
}
