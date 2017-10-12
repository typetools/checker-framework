import org.checkerframework.checker.units.UnitsTools;
import org.checkerframework.checker.units.qual.Prefix;
import org.checkerframework.checker.units.qual.s;
import qual.Frequency;
import qual.Hz;
import qual.kHz;

class UnitsExtensionDemo {
    @Hz int frq;

    void bad() {
        // Error! Unqualified value assigned to a @Hz value.
        // :: error: (assignment.type.incompatible)
        frq = 5;

        // suppress all warnings issued by the units checker for the d1 assignment statement
        @SuppressWarnings("units")
        @Hz int d1 = 9;

        // specifically suppress warnings related to any frequency units for the d2 assigment
        // statement
        @SuppressWarnings("frequency")
        @Hz int d2 = 10;
    }

    // specifically suppresses warnings for the hz annotation for the toHz method
    @SuppressWarnings("hz")
    static @Hz int toHz(int hz) {
        return hz;
    }

    void good() {
        frq = toHz(9);

        @s double time = 5 * UnitsTools.s;
        @Hz double freq2 = 20 / time;
    }

    void auto(@s int time) {
        // The @Hz annotation is automatically added to the result
        // of the division, because we provide class FrequencyRelations.
        frq = 99 / time;
    }

    public static void main(String[] args) {
        @Hz int hertz = toHz(20);
        @s int seconds = 5 * UnitsTools.s;

        @SuppressWarnings("units")
        @s(Prefix.milli) int millisec = 10;

        @SuppressWarnings("hz")
        @kHz int kilohertz = 30;

        @Hz int resultHz = hertz + 20 / seconds;
        System.out.println(resultHz);

        @kHz int resultkHz = kilohertz + 50 / millisec;
        System.out.println(resultkHz);

        // this demonstrates the type hierarchy resolution: the common supertype of Hz and kHz is
        // Frequency, so this statement will pass
        @Frequency int okTernaryAssign = seconds > 10 ? hertz : kilohertz;

        // on the other hand, this statement expects the right hand side to be a Hz, so it will fail
        // :: error: (assignment.type.incompatible)
        @Hz int badTernaryAssign = seconds > 10 ? hertz : kilohertz;
    }
}
