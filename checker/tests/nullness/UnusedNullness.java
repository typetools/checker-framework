import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.Unused;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

// TODO: feature request: the Nullness Checker should be aware of
// the @Unused annotation.
// This is difficult to implement: one needs to determine the correct
// AnnotatedTypeFactory for the "when" type system and use it
// to determine the right annotated type. We currently don't have
// a mechanism to do this.
//
// @skip-test
public class UnusedNullness {

    @SubtypeOf({})
    @Target(ElementType.TYPE_USE)
    public @interface Prototype {}

    @Unused(when = Prototype.class)
    public Object ppt;

    protected @Prototype UnusedNullness() {
        // It should be legal to initialize an unused field to null in
        // a constructor with @Prototype receiver.
        this.ppt = null;
    }

    protected @Prototype UnusedNullness(int disambiguate_overloading) {
        // It should be legal to NOT initialize an unused field in
        // a constructor with @Prototype receiver.
    }

    protected void protometh(@Prototype UnusedNullness this) {
        // It should be legal to initialize the unused field to null in
        // a method with @Prototype receiver.
        this.ppt = null;
    }

    protected void meth() {
        // Otherwise it's not legal.
        // :: error: (assignment.type.incompatible)
        this.ppt = null;
    }

    protected void useUnusedField1(@Prototype UnusedNullness this) {
        // :: error: (assignment.type.incompatible)
        @NonNull Object x = this.ppt;
    }

    protected void useUnusedField2() {
        @NonNull Object x = this.ppt;
    }
}
