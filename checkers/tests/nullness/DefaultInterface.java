import checkers.nullness.quals.*;
import checkers.quals.DefaultQualifier;
@DefaultQualifier(NonNull.class)
interface Foo {
    void foo(String a, String b);
}

@DefaultQualifier(NonNull.class)
class DefaultInterface {

    public void test() {

        @Nullable Foo foo = null;

    }


}
