package foo;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Foo {

    //  @Nullable Object theObject;

    Foo(@Nullable Object theObject) {
        //   this.theObject = theObject;
    }

    @SuppressWarnings("contracts.conditional.postcondition.not.satisfied")
    @EnsuresNonNullIf(
        expression = {"theObject", "getTheObject()"},
        result = true
    )
    public boolean hasTheObject() {
        //return theObject != null;
        return false;
    }

    @Nullable public Object getTheObject() {
        // return theObject;
        return null;
    }
}
