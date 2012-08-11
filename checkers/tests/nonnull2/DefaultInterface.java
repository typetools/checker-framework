import checkers.nullness.quals.*;
import checkers.quals.DefaultQualifier;

@DefaultQualifier(checkers.nullness.quals.NonNull.class)
interface Foo {
    void foo(String a, String b);
}

@DefaultQualifier(checkers.nullness.quals.NonNull.class)
class DefaultInterface {

    public void test() {

        @Nullable Foo foo = null;

    }


}
