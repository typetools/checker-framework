import org.checkerframework.checker.i18n.qual.*;

// @C <: @B <: @A

// Testing Rule 1 (constructor declaration type <: class type)
@UnknownLocalizableKey class Issue2163FinalAA {
    @UnknownLocalizableKey Issue2163FinalAA() {}
}

@UnknownLocalizableKey class Issue2163FinalAB {
    @LocalizableKey Issue2163FinalAB() {}
}

@UnknownLocalizableKey class Issue2163FinalAC {
    @LocalizableKeyBottom Issue2163FinalAC() {}
}

@LocalizableKey class Issue2163FinalBA {
    // :: error: (type.invalid.annotations.on.use)
    @UnknownLocalizableKey Issue2163FinalBA() {}
}

@LocalizableKey class Issue2163FinalBB {
    @LocalizableKey Issue2163FinalBB() {}
}

@LocalizableKey class Issue2163FinalBC {
    @LocalizableKeyBottom Issue2163FinalBC() {}
}

@LocalizableKeyBottom class Issue2163FinalCA {
    // :: error: (type.invalid.annotations.on.use)
    @UnknownLocalizableKey Issue2163FinalCA() {}
}

@LocalizableKeyBottom class Issue2163FinalCB {
    // :: error: (type.invalid.annotations.on.use)
    @LocalizableKey Issue2163FinalCB() {}
}

@LocalizableKeyBottom class Issue2163FinalCC {
    @LocalizableKeyBottom Issue2163FinalCC() {}
}

// Testing Rule 2 (Issue type cast warning if constuctor declaration type <: invocation type)
class Issue2163FinalAAClient {
    void test() {
        new @UnknownLocalizableKey Issue2163FinalAA();
        // :: warning: (cast.unsafe)
        new @LocalizableKey Issue2163FinalAA();
        // :: warning: (cast.unsafe)
        new @LocalizableKeyBottom Issue2163FinalAA();
    }
}

// Testing Default
class Issue2163FinalBCClient {
    // :: error: (assignment.type.incompatible)
    @LocalizableKeyBottom Issue2163FinalBC obj = new Issue2163FinalBC();
}
