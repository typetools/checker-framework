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

    // TODO: see comment in Constructors.java
    @Immutable
    ImmutableObject() {}

    static void isReadOnly(@ReadOnly ImmutableObject o) { }
    static void isImmutable(@Immutable ImmutableObject o) { }
    //:: error: (type.invalid)
    static void isMutable(@Mutable ImmutableObject o) { }

    void defaultMethod() { }
    void readOnlyReceiver(@ReadOnly ImmutableObject this) { }
    void immutableReceiver(@Immutable ImmutableObject this) { }

    void testDefaultCall() {
        isReadOnly(this);
        isImmutable(this);
        //:: error: (argument.type.incompatible)
        isMutable(this);

        defaultMethod();
        readOnlyReceiver();
        immutableReceiver();

        this.defaultMethod();
        this.readOnlyReceiver();
        this.immutableReceiver();

        @ReadOnly ImmutableObject readOnly = (@ReadOnly ImmutableObject)null;
        readOnly.defaultMethod();
        readOnly.readOnlyReceiver();
        //:: error: (method.invocation.invalid)
        readOnly.immutableReceiver();
        isReadOnly(readOnly);
        //:: error: (argument.type.incompatible)
        isImmutable(readOnly);
        //:: error: (argument.type.incompatible)
        isMutable(readOnly);

        @Immutable ImmutableObject immutable = (@Immutable ImmutableObject)null;
        immutable.defaultMethod();
        immutable.readOnlyReceiver();
        immutable.immutableReceiver();
        isReadOnly(immutable);
        isImmutable(immutable);
        //:: error: (argument.type.incompatible)
        isMutable(immutable);
    }

    void testReadOnlyCall(@ReadOnly ImmutableObject this) {
        isReadOnly(this);
        isImmutable(this);
        //:: error: (argument.type.incompatible)
        isMutable(this);

        defaultMethod();
        readOnlyReceiver();
        immutableReceiver();

        this.defaultMethod();
        this.readOnlyReceiver();
        this.immutableReceiver();

        @ReadOnly ImmutableObject readOnly = (@ReadOnly ImmutableObject)null;
        readOnly.defaultMethod();
        readOnly.readOnlyReceiver();
        //:: error: (method.invocation.invalid)
        readOnly.immutableReceiver();
        isReadOnly(readOnly);
        //:: error: (argument.type.incompatible)
        isImmutable(readOnly);
        //:: error: (argument.type.incompatible)
        isMutable(readOnly);

        @Immutable ImmutableObject immutable = (@Immutable ImmutableObject)null;
        immutable.defaultMethod();
        immutable.readOnlyReceiver();
        immutable.immutableReceiver();
        isReadOnly(immutable);
        isImmutable(immutable);
        //:: error: (argument.type.incompatible)
        isMutable(immutable);
    }

    void testImmutableCall(@Immutable ImmutableObject this) {
        isReadOnly(this);
        isImmutable(this);
        //:: error: (argument.type.incompatible)
        isMutable(this);

        defaultMethod();
        readOnlyReceiver();
        immutableReceiver();

        this.defaultMethod();
        this.readOnlyReceiver();
        this.immutableReceiver();

        @ReadOnly ImmutableObject readOnly = (@ReadOnly ImmutableObject)null;
        readOnly.defaultMethod();
        readOnly.readOnlyReceiver();
        //:: error: (method.invocation.invalid)
        readOnly.immutableReceiver();
        isReadOnly(readOnly);
        //:: error: (argument.type.incompatible)
        isImmutable(readOnly);
        //:: error: (argument.type.incompatible)
        isMutable(readOnly);

        @Immutable ImmutableObject immutable = (@Immutable ImmutableObject)null;
        immutable.defaultMethod();
        immutable.readOnlyReceiver();
        immutable.immutableReceiver();
        isReadOnly(immutable);
        isImmutable(immutable);
        //:: error: (argument.type.incompatible)
        isMutable(immutable);
    }

    ImmutableObject getNew() { return null; }

    void testCreation() {
        ImmutableObject o = new ImmutableObject();
        isReadOnly(o);
        isImmutable(o);
        //:: error: (argument.type.incompatible)
        isMutable(o);

        isReadOnly(getNew());
        isImmutable(getNew());
        //:: error: (argument.type.incompatible)
        isMutable(getNew());

        isReadOnly(new ImmutableObject());
        isImmutable(new ImmutableObject());
        //:: error: (argument.type.incompatible)
        isMutable(new ImmutableObject());
    }
}
