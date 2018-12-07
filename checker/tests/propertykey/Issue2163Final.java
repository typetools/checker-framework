// @skip-test

import org.checkerframework.checker.propkey.qual.*;

// @C <: @B <: @A

// Testing Rule 1 (constructor declaration type <: class type)
@UnknownPropertyKey class Issue2163FinalAA {
    @UnknownPropertyKey Issue2163FinalAA() {}
}

@UnknownPropertyKey class Issue2163FinalAB {
    @PropertyKey Issue2163FinalAB() {}
}

@UnknownPropertyKey class Issue2163FinalAC {
    @PropertyKeyBottom Issue2163FinalAC() {}
}

@PropertyKey class Issue2163FinalBA {
    // :: error: (type.invalid.annotations.on.use)
    @UnknownPropertyKey Issue2163FinalBA() {}
}

@PropertyKey class Issue2163FinalBB {
    @PropertyKey Issue2163FinalBB() {}
}

@PropertyKey class Issue2163FinalBC {
    @PropertyKeyBottom Issue2163FinalBC() {}
}

@PropertyKeyBottom class Issue2163FinalCA {
    // :: error: (type.invalid.annotations.on.use)
    @UnknownPropertyKey Issue2163FinalCA() {}
}

@PropertyKeyBottom class Issue2163FinalCB {
    // :: error: (type.invalid.annotations.on.use)
    @PropertyKey Issue2163FinalCB() {}
}

@PropertyKeyBottom class Issue2163FinalCC {
    @PropertyKeyBottom Issue2163FinalCC() {}
}

// Testing Rule 2 (Issue type cast warning if constuctor declaration type <: invocation type)
class Issue2163FinalAAClient {
    void test() {
        new @UnknownPropertyKey Issue2163FinalAA();
        // :: warning: (cast.unsafe)
        new @PropertyKey Issue2163FinalAA();
        // :: warning: (cast.unsafe)
        new @PropertyKeyBottom Issue2163FinalAA();
    }
}

// Testing Default
class Issue2163FinalBCClient {
    // :: error: (assignment.type.incompatible)
    @PropertyKeyBottom Issue2163FinalBC obj = new Issue2163FinalBC();
}
