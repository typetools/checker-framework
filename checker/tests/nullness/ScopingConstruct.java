import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("initialization.fields.uninitialized")
class ScopingConstruct {

    // TODO: add nested classes within these two?
    static class StaticNested {
        static class NestedNested {}

        class NestedInner {}
    }

    class Inner {
        // This is a Java error.
        // static class InnerNested {}

        class InnerInner {}
    }

    StaticNested sn;

    @Nullable StaticNested nsn;

    Inner i;

    @Nullable Inner ni;

    ScopingConstruct.StaticNested scsn;

    // This is a Java error.
    // @Nullable ScopingConstruct.StaticNested nscsn;

    ScopingConstruct.@Nullable StaticNested scnsn;

    // This is a Java error.
    // ScopingConstruct.@Nullable StaticNested.NestedNested scnsnnn;

    // This is a Java error.
    // ScopingConstruct.@Nullable StaticNested.@Nullable NestedNested scnsnnnn;

    // :: error: (nullness.on.outer)
    ScopingConstruct.@Nullable StaticNested.NestedInner scnsnni;

    // :: error: (nullness.on.outer)
    ScopingConstruct.@Nullable StaticNested.@Nullable NestedInner scnsnnni;

    ScopingConstruct.Inner sci;

    ScopingConstruct.Inner.InnerInner sciii;

    ScopingConstruct.Inner.@Nullable InnerInner scinii;

    // :: error: (nullness.on.outer)
    @Nullable ScopingConstruct.Inner nsci;

    // :: error: (nullness.on.outer)
    @Nullable ScopingConstruct.Inner.InnerInner nsciii;

    // :: error: (nullness.on.outer)
    @Nullable ScopingConstruct.Inner.@Nullable InnerInner nscinii;

    ScopingConstruct.@Nullable Inner scni;

    // :: error: (nullness.on.outer)
    ScopingConstruct.@Nullable Inner.InnerInner scniii;

    // :: error: (nullness.on.outer)
    ScopingConstruct.@Nullable Inner.@Nullable InnerInner scninii;

    ScopingConstruct.StaticNested.NestedInner scsnni;

    ScopingConstruct.StaticNested.@Nullable NestedInner scsnnni;

    // This is a Java error.
    // @Nullable ScopingConstruct.StaticNested.NestedInner nscsnni;

    // This is a Java error.
    // @Nullable ScopingConstruct.StaticNested.@Nullable NestedInner nscsnnni;

    // This is a Java error.
    // @Nullable ScopingConstruct.@Nullable StaticNested.NestedInner nscnsnni;

    // This is a Java error.
    // @Nullable ScopingConstruct.@Nullable StaticNested.@Nullable NestedInner nscnsnnni;

    ScopingConstruct.Inner @Nullable [] scina;

    ScopingConstruct.Inner.InnerInner @Nullable [] sciiina;

    ScopingConstruct.Inner.@Nullable InnerInner @Nullable [] sciniina;

    // :: error: (nullness.on.outer)
    @Nullable ScopingConstruct.Inner @Nullable [] nscina;

    // :: error: (nullness.on.outer)
    @Nullable ScopingConstruct.Inner.InnerInner @Nullable [] nsciiina;

    // :: error: (nullness.on.outer)
    @Nullable ScopingConstruct.Inner.@Nullable InnerInner @Nullable [] nsciniina;

    ScopingConstruct.@Nullable Inner @Nullable [] scnina;

    // :: error: (nullness.on.outer)
    ScopingConstruct.@Nullable Inner.InnerInner @Nullable [] scniina;

    // :: error: (nullness.on.outer)
    ScopingConstruct.@Nullable Inner.@Nullable InnerInner @Nullable [] scniniina;

    ScopingConstruct.Inner sci() {
        throw new Error("not implemented");
    }

    ScopingConstruct.Inner.InnerInner sciii() {
        throw new Error("not implemented");
    }

    ScopingConstruct.Inner.@Nullable InnerInner scinii() {
        throw new Error("not implemented");
    }

    // :: error: (nullness.on.outer)
    @Nullable ScopingConstruct.Inner nsci() {
        throw new Error("not implemented");
    }

    // :: error: (nullness.on.outer)
    @Nullable ScopingConstruct.Inner.InnerInner nsciii() {
        throw new Error("not implemented");
    }

    // :: error: (nullness.on.outer)
    @Nullable ScopingConstruct.Inner.@Nullable InnerInner nscinii() {
        throw new Error("not implemented");
    }

    ScopingConstruct.@Nullable Inner scni() {
        throw new Error("not implemented");
    }

    // :: error: (nullness.on.outer)
    ScopingConstruct.@Nullable Inner.InnerInner scniii() {
        throw new Error("not implemented");
    }

    // :: error: (nullness.on.outer)
    ScopingConstruct.@Nullable Inner.@Nullable InnerInner scninii() {
        throw new Error("not implemented");
    }

    ScopingConstruct.Inner @Nullable [] scin() {
        throw new Error("not implemented");
    }

    ScopingConstruct.Inner.InnerInner @Nullable [] sciiin() {
        throw new Error("not implemented");
    }

    ScopingConstruct.Inner.@Nullable InnerInner @Nullable [] sciniin() {
        throw new Error("not implemented");
    }

    // :: error: (nullness.on.outer)
    @Nullable ScopingConstruct.Inner @Nullable [] nscin() {
        throw new Error("not implemented");
    }

    // :: error: (nullness.on.outer)
    @Nullable ScopingConstruct.Inner.InnerInner @Nullable [] nsciiin() {
        throw new Error("not implemented");
    }

    // :: error: (nullness.on.outer)
    @Nullable ScopingConstruct.Inner.@Nullable InnerInner @Nullable [] nsciniin() {
        throw new Error("not implemented");
    }

    ScopingConstruct.@Nullable Inner @Nullable [] scnin() {
        throw new Error("not implemented");
    }

    // :: error: (nullness.on.outer)
    ScopingConstruct.@Nullable Inner.InnerInner @Nullable [] scniiin() {
        throw new Error("not implemented");
    }

    // :: error: (nullness.on.outer)
    ScopingConstruct.@Nullable Inner.@Nullable InnerInner @Nullable [] scniniin() {
        throw new Error("not implemented");
    }
}
