import org.checkerframework.framework.testchecker.typedeclbounds.quals.*;

import java.util.ArrayList;
import java.util.List;

// Test the @S1 upperbound applied to string conversion
public class StringConcatConversion<T> {

    @Top List<? extends T> ts = new ArrayList<>();

    // :: error: (type.invalid.annotations.on.use)
    @Top String topString;

    @Bottom String bottomString;

    void foo(@Top T topT, @Bottom T bottomT) {
        throwException("test normal top to bottom conversion" + ts);
        throwException("test type variable" + topT);
        throwException("test wildcard" + ts.get(0));

        // the converted string of topT has type @S1
        // :: error: (compound.assignment.type.incompatible)
        bottomString += topT;

        // the converted string of bottomT has type @Bottom
        bottomString += bottomT;
    }

    void throwException(@S1 String s) {}
}
