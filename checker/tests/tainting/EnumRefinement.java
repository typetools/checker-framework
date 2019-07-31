import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class EnumRefinement {

    enum MyEnum {
        CONST1,
        CONST2,
    }

    void method(@Untainted MyEnum e1, @Tainted MyEnum e2) {
        e1.compareTo(e2);
    }

    // :: warning: (inconsistent.constructor.type)
    @Untainted enum MyUntaintedEnum {
        CONST1,
        CONST2,
    }

    // :: error: (type.invalid.annotations.on.use)
    void method(@Untainted MyUntaintedEnum e1, @Tainted MyUntaintedEnum e2) {
        // Check that qualifier upper bound on MyUntaintedEnum is respected.
        // :: error: (argument.type.incompatible)
        e1.compareTo(e2);
    }
}
