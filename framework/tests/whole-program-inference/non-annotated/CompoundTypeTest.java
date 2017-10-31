import testlib.wholeprograminference.qual.*;

public class CompoundTypeTest {
    // The default type for fields is @DefaultType.
    Object[] field;

    void assign() {
        field = getCompoundType();
    }

    void test() {
        // :: error: (argument.type.incompatible)
        expectsCompoundType(field);
    }

    void expectsCompoundType(@Sibling1 Object @Sibling2 [] obj) {}

    @Sibling1 Object @Sibling2 [] getCompoundType() {
        // :: warning: (cast.unsafe)
        @Sibling1 Object @Sibling2 [] out = (@Sibling1 Object @Sibling2 []) new Object[1];
        return out;
    }
}
