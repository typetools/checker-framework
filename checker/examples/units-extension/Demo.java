import org.checkerframework.checker.units.qual.*;

public class Demo {
    @Hz int frq;

    void bad() {
	// Error! Unqualified value assigned to a @Hz value.
	frq = 5;

	@SuppressWarnings("units")
	@Hz int d1 = 9;

	@SuppressWarnings("frequency")
	@Hz int d2 = 10;
    }

    @SuppressWarnings("hz")
    @Hz int toHz(int hz) {
	return hz;
    }

    void good() {
	frq = toHz(9);
    }

    void auto(@s int time) {
	// The @Hz annotation is automatically added to the result
	// of the division, because we provide class FrequencyRelations.
	frq = 99 / time;
    }

}