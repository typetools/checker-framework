import checkers.igj.quals.*;

/**
 * Tests behaviour for Immutable Manifest object.
 *
 * Keep in mind that the default receiver type of an immutable
 * object is ReadOnly not Immutable
 *
 * Also, as a special rule this escapes as Immutable.
 *
 */
// TODO: Ask about immutable object escaping in general
@Immutable
public class ImmutableObject {

    static void isReadOnly(@ReadOnly ImmutableObject o) { }
    static void isImmutable(@Immutable ImmutableObject o) { }
    static void isMutable(@Mutable ImmutableObject o) { }   // should emit error

    void defaultMethod() { }
    void readOnlyReceiver(@ReadOnly ImmutableObject this) { }
    void immutableReceiver(@Immutable ImmutableObject this) { }

    void testDefaultCall() {
        isReadOnly(this);
        isImmutable(this);
        isMutable(this);    // should emit error

        defaultMethod();
        readOnlyReceiver();
        immutableReceiver();

        this.defaultMethod();
        this.readOnlyReceiver();
        this.immutableReceiver();

        @ReadOnly ImmutableObject readOnly = (@ReadOnly ImmutableObject)null;
        readOnly.defaultMethod();
        readOnly.readOnlyReceiver();
        readOnly.immutableReceiver();  // should emit error
        isReadOnly(readOnly);
        isImmutable(readOnly);  // should emit error
        isMutable(readOnly);    // should emit error

        @Immutable ImmutableObject immutable = (@Immutable ImmutableObject)null;
        immutable.defaultMethod();
        immutable.readOnlyReceiver();
        immutable.immutableReceiver();
        isReadOnly(immutable);
        isImmutable(immutable);
        isMutable(immutable);    // should emit error
    }

    void testReadOnlyCall(@ReadOnly ImmutableObject this) {
        isReadOnly(this);
        isImmutable(this);
        isMutable(this);    // should emit error

        defaultMethod();
        readOnlyReceiver();
        immutableReceiver();

        this.defaultMethod();
        this.readOnlyReceiver();
        this.immutableReceiver();

        @ReadOnly ImmutableObject readOnly = (@ReadOnly ImmutableObject)null;
        readOnly.defaultMethod();
        readOnly.readOnlyReceiver();
        readOnly.immutableReceiver();  // should emit error
        isReadOnly(readOnly);
        isImmutable(readOnly);  // should emit error
        isMutable(readOnly);    // should emit error

        @Immutable ImmutableObject immutable = (@Immutable ImmutableObject)null;
        immutable.defaultMethod();
        immutable.readOnlyReceiver();
        immutable.immutableReceiver();
        isReadOnly(immutable);
        isImmutable(immutable);
        isMutable(immutable);    // should emit error
    }

    void testImmutableCall(@Immutable ImmutableObject this) {
        isReadOnly(this);
        isImmutable(this);
        isMutable(this);    // should emit error

        defaultMethod();
        readOnlyReceiver();
        immutableReceiver();

        this.defaultMethod();
        this.readOnlyReceiver();
        this.immutableReceiver();

        @ReadOnly ImmutableObject readOnly = (@ReadOnly ImmutableObject)null;
        readOnly.defaultMethod();
        readOnly.readOnlyReceiver();
        readOnly.immutableReceiver();  // should emit error
        isReadOnly(readOnly);
        isImmutable(readOnly);  // should emit error
        isMutable(readOnly);    // should emit error

        @Immutable ImmutableObject immutable = (@Immutable ImmutableObject)null;
        immutable.defaultMethod();
        immutable.readOnlyReceiver();
        immutable.immutableReceiver();
        isReadOnly(immutable);
        isImmutable(immutable);
        isMutable(immutable);    // should emit error
    }

    ImmutableObject getNew() { return null; }

    void testCreation() {
        ImmutableObject o = new ImmutableObject();
        isReadOnly(o);
        isImmutable(o);
        isMutable(o);   // should emit error

        isReadOnly(getNew());
        isImmutable(getNew());
        isMutable(getNew());   // should emit error

        isReadOnly(new ImmutableObject());
        isImmutable(new ImmutableObject());
        isMutable(new ImmutableObject());   // should emit error
    }
}
