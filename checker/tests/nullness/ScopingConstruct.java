import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("initialization.field.uninitialized")
public class ScopingConstruct {

    // TODO: add nested classes within these two?
    static class StaticNested implements AutoCloseable {
        public void close() {}

        static class NestedNested implements AutoCloseable {
            public void close() {}
        }

        class NestedInner implements AutoCloseable {
            public void close() {}
        }
    }

    class Inner implements AutoCloseable {
        public void close() {}

        // This is a Java error.
        // static class InnerNested {}

        class InnerInner implements AutoCloseable {
            public void close() {}
        }
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

    ///
    /// Formal parameters
    ///

    void fsn(StaticNested sn) {}

    void fnsn(@Nullable StaticNested nsn) {}

    void fi(Inner i) {}

    void fni(@Nullable Inner ni) {}

    void fscsn(ScopingConstruct.StaticNested scsn) {}

    void fscnsn(ScopingConstruct.@Nullable StaticNested scnsn) {}

    // :: error: (nullness.on.outer)
    void fscnsnni(ScopingConstruct.@Nullable StaticNested.NestedInner scnsnni) {}

    // :: error: (nullness.on.outer)
    void fscnsnnni(ScopingConstruct.@Nullable StaticNested.@Nullable NestedInner scnsnnni) {}

    void fsci(ScopingConstruct.Inner sci) {}

    void fsciii(ScopingConstruct.Inner.InnerInner sciii) {}

    void fscinii(ScopingConstruct.Inner.@Nullable InnerInner scinii) {}

    // :: error: (nullness.on.outer)
    void fnsci(@Nullable ScopingConstruct.Inner nsci) {}

    // :: error: (nullness.on.outer)
    void fnsciii(@Nullable ScopingConstruct.Inner.InnerInner nsciii) {}

    // :: error: (nullness.on.outer)
    void fnscinii(@Nullable ScopingConstruct.Inner.@Nullable InnerInner nscinii) {}

    void fscni(ScopingConstruct.@Nullable Inner scni) {}

    // :: error: (nullness.on.outer)
    void fscniii(ScopingConstruct.@Nullable Inner.InnerInner scniii) {}

    // :: error: (nullness.on.outer)
    void fscninii(ScopingConstruct.@Nullable Inner.@Nullable InnerInner scninii) {}

    void fscsnni(ScopingConstruct.StaticNested.NestedInner scsnni) {}

    void fscsnnni(ScopingConstruct.StaticNested.@Nullable NestedInner scsnnni) {}

    ///
    /// Local variables
    ///

    void lvsn() {
        StaticNested sn;
    }

    void lvnsn() {
        @Nullable StaticNested nsn;
    }

    void lvi() {
        Inner i;
    }

    void lvni() {
        @Nullable Inner ni;
    }

    void lvscsn() {
        ScopingConstruct.StaticNested scsn;
    }

    void lvscnsn() {
        ScopingConstruct.@Nullable StaticNested scnsn;
    }

    void lvscnsnni() {
        // :: error: (nullness.on.outer)
        ScopingConstruct.@Nullable StaticNested.NestedInner scnsnni;
    }

    void lvscnsnnni() {
        // :: error: (nullness.on.outer)
        ScopingConstruct.@Nullable StaticNested.@Nullable NestedInner scnsnnni;
    }

    void lvsci() {
        ScopingConstruct.Inner sci;
    }

    void lvsciii() {
        ScopingConstruct.Inner.InnerInner sciii;
    }

    void lvscinii() {
        ScopingConstruct.Inner.@Nullable InnerInner scinii;
    }

    void lvnsci() {
        // :: error: (nullness.on.outer)
        @Nullable ScopingConstruct.Inner nsci;
    }

    void lvnsciii() {
        // :: error: (nullness.on.outer)
        @Nullable ScopingConstruct.Inner.InnerInner nsciii;
    }

    void lvnscinii() {
        // :: error: (nullness.on.outer)
        @Nullable ScopingConstruct.Inner.@Nullable InnerInner nscinii;
    }

    void lvscni() {
        ScopingConstruct.@Nullable Inner scni;
    }

    void lvscniii() {
        // :: error: (nullness.on.outer)
        ScopingConstruct.@Nullable Inner.InnerInner scniii;
    }

    void lvscninii() {
        // :: error: (nullness.on.outer)
        ScopingConstruct.@Nullable Inner.@Nullable InnerInner scninii;
    }

    void lvscsnni() {
        ScopingConstruct.StaticNested.NestedInner scsnni;
    }

    void lvscsnnni() {
        ScopingConstruct.StaticNested.@Nullable NestedInner scsnnni;
    }

    ///
    /// Resource variables
    ///

    void rvsn() {
        try (StaticNested sn = null) {}
    }

    void rvnsn() {
        try (@Nullable StaticNested nsn = null) {}
    }

    void rvi() {
        try (Inner i = null) {}
    }

    void rvni() {
        try (@Nullable Inner ni = null) {}
    }

    void rvscsn() {
        try (ScopingConstruct.StaticNested scsn = null) {}
    }

    void rvscnsn() {
        try (ScopingConstruct.@Nullable StaticNested scnsn = null) {}
    }

    void rvscnsnni() {
        // :: error: (nullness.on.outer)
        try (ScopingConstruct.@Nullable StaticNested.NestedInner scnsnni = null) {}
    }

    void rvscnsnnni() {
        // :: error: (nullness.on.outer)
        try (ScopingConstruct.@Nullable StaticNested.@Nullable NestedInner scnsnnni = null) {}
    }

    void rvsci() {
        try (ScopingConstruct.Inner sci = null) {}
    }

    void rvsciii() {
        try (ScopingConstruct.Inner.InnerInner sciii = null) {}
    }

    void rvscinii() {
        try (ScopingConstruct.Inner.@Nullable InnerInner scinii = null) {}
    }

    void rvnsci() {
        // :: error: (nullness.on.outer)
        try (@Nullable ScopingConstruct.Inner nsci = null) {}
    }

    void rvnsciii() {
        // :: error: (nullness.on.outer)
        try (@Nullable ScopingConstruct.Inner.InnerInner nsciii = null) {}
    }

    void rvnscinii() {
        // :: error: (nullness.on.outer)
        try (@Nullable ScopingConstruct.Inner.@Nullable InnerInner nscinii = null) {}
    }

    void rvscni() {
        try (ScopingConstruct.@Nullable Inner scni = null) {}
    }

    void rvscniii() {
        // :: error: (nullness.on.outer)
        try (ScopingConstruct.@Nullable Inner.InnerInner scniii = null) {}
    }

    void rvscninii() {
        // :: error: (nullness.on.outer)
        try (ScopingConstruct.@Nullable Inner.@Nullable InnerInner scninii = null) {}
    }

    void rvscsnni() {
        try (ScopingConstruct.StaticNested.NestedInner scsnni = null) {}
    }

    void rvscsnnni() {
        try (ScopingConstruct.StaticNested.@Nullable NestedInner scsnnni = null) {}
    }

    ///
    /// For variables
    ///

    void fvsn() {
        for (StaticNested sn = null; ; ) {}
    }

    void fvnsn() {
        for (@Nullable StaticNested nsn = null; ; ) {}
    }

    void fvi() {
        for (Inner i = null; ; ) {}
    }

    void fvni() {
        for (@Nullable Inner ni = null; ; ) {}
    }

    void fvscsn() {
        for (ScopingConstruct.StaticNested scsn = null; ; ) {}
    }

    void fvscnsn() {
        for (ScopingConstruct.@Nullable StaticNested scnsn = null; ; ) {}
    }

    void fvscnsnni() {
        // :: error: (nullness.on.outer)
        for (ScopingConstruct.@Nullable StaticNested.NestedInner scnsnni = null; ; ) {}
    }

    void fvscnsnnni() {
        // :: error: (nullness.on.outer)
        for (ScopingConstruct.@Nullable StaticNested.@Nullable NestedInner scnsnnni = null; ; ) {}
    }

    void fvsci() {
        for (ScopingConstruct.Inner sci = null; ; ) {}
    }

    void fvsciii() {
        for (ScopingConstruct.Inner.InnerInner sciii = null; ; ) {}
    }

    void fvscinii() {
        for (ScopingConstruct.Inner.@Nullable InnerInner scinii = null; ; ) {}
    }

    void fvnsci() {
        // :: error: (nullness.on.outer)
        for (@Nullable ScopingConstruct.Inner nsci = null; ; ) {}
    }

    void fvnsciii() {
        // :: error: (nullness.on.outer)
        for (@Nullable ScopingConstruct.Inner.InnerInner nsciii = null; ; ) {}
    }

    void fvnscinii() {
        // :: error: (nullness.on.outer)
        for (@Nullable ScopingConstruct.Inner.@Nullable InnerInner nscinii = null; ; ) {}
    }

    void fvscni() {
        for (ScopingConstruct.@Nullable Inner scni = null; ; ) {}
    }

    void fvscniii() {
        // :: error: (nullness.on.outer)
        for (ScopingConstruct.@Nullable Inner.InnerInner scniii = null; ; ) {}
    }

    void fvscninii() {
        // :: error: (nullness.on.outer)
        for (ScopingConstruct.@Nullable Inner.@Nullable InnerInner scninii = null; ; ) {}
    }

    void fvscsnni() {
        for (ScopingConstruct.StaticNested.NestedInner scsnni = null; ; ) {}
    }

    void fvscsnnni() {
        for (ScopingConstruct.StaticNested.@Nullable NestedInner scsnnni = null; ; ) {}
    }
}
