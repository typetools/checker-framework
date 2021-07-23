import org.checkerframework.checker.testchecker.ainfer.qual.Sibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.Sibling2;

public class CompoundTypeTest {
    // The default type for fields is @DefaultType.
    Object[] field;

    void assign() {
        field = getCompoundType();
    }

    void test() {
        // :: warning: (argument.type.incompatible)
        expectsCompoundType(field);
    }

    void expectsCompoundType(@Sibling1 Object @Sibling2 [] obj) {}

    @Sibling1 Object @Sibling2 [] getCompoundType() {
        @SuppressWarnings("cast.unsafe")
        @Sibling1 Object @Sibling2 [] out = (@Sibling1 Object @Sibling2 []) new Object[1];
        return out;
    }
}
