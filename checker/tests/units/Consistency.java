import org.checkerframework.checker.units.*;
import org.checkerframework.checker.units.qual.*;

/**
 * One possible future extension is adding method annotations to check for consistency of arguments.
 * This is not implemented yet; send us a message if you think this would help you!
 *
 * @skip-test
 */
public class Consistency {

    @UnitsSame({0, 1})
    @UnitsProduct({0, 1, -1})
    @Area int calcArea(@Length int width, @Length int height) {
        return width * height;
    }

    void use() {
        @m int m1, m2;
        m1 = UnitsTools.toMeter(5);
        m2 = UnitsTools.toMeter(51);

        @km int km1, km2;
        km1 = UnitsTools.toMeter(5);
        km2 = UnitsTools.toMeter(5);

        @m2 int msq;
        @km2 int kmsq;

        // good
        msq = calcArea(m1, m2);

        // :: bad args
        msq = calcArea(m1, km2);

        // :: bad return
        kmsq = calcArea(m1, m2);

        // good
        kmsq = calcArea(km1, km2);
    }
}
