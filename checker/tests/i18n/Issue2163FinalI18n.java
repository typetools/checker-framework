import org.checkerframework.checker.i18n.qual.*;

// @C <: @B <: @A

// Testing Rule 1 (constructor declaration type <: class type)
@SuppressWarnings("anno.on.irrelevant")
@UnknownLocalizableKey class Issue2163FinalAA {
  @UnknownLocalizableKey Issue2163FinalAA() {}
}

@SuppressWarnings("anno.on.irrelevant")
@UnknownLocalizableKey class Issue2163FinalAB {
  // :: error: (super.invocation) :: warning: (inconsistent.constructor.type)
  @LocalizableKey Issue2163FinalAB() {}
}

@SuppressWarnings("anno.on.irrelevant")
@UnknownLocalizableKey class Issue2163FinalAC {
  // :: error: (super.invocation) :: warning: (inconsistent.constructor.type)
  @LocalizableKeyBottom Issue2163FinalAC() {}
}

@SuppressWarnings("anno.on.irrelevant")
@LocalizableKey class Issue2163FinalBA {
  @UnknownLocalizableKey Issue2163FinalBA() {}
}

@SuppressWarnings("anno.on.irrelevant")
@LocalizableKey class Issue2163FinalBB {
  // :: error: (super.invocation) :: warning: (inconsistent.constructor.type)
  @LocalizableKey Issue2163FinalBB() {}
}

@SuppressWarnings("anno.on.irrelevant")
@LocalizableKey class Issue2163FinalBC {
  // :: error: (super.invocation) :: warning: (inconsistent.constructor.type)
  @LocalizableKeyBottom Issue2163FinalBC() {}
}

@SuppressWarnings("anno.on.irrelevant")
@LocalizableKeyBottom class Issue2163FinalCA {
  @UnknownLocalizableKey Issue2163FinalCA() {}
}

@SuppressWarnings("anno.on.irrelevant")
@LocalizableKeyBottom class Issue2163FinalCB {
  // :: warning: (inconsistent.constructor.type) :: error: (super.invocation)
  @LocalizableKey Issue2163FinalCB() {}
}

@SuppressWarnings("anno.on.irrelevant")
@LocalizableKeyBottom class Issue2163FinalCC {
  // :: error: (super.invocation) :: warning: (inconsistent.constructor.type)
  @LocalizableKeyBottom Issue2163FinalCC() {}
}

// Testing Rule 2 (Issue type cast warning if constuctor declaration type <: invocation type)
@SuppressWarnings("anno.on.irrelevant")
class Issue2163FinalAAClient {
  void test() {
    new @UnknownLocalizableKey Issue2163FinalAA();
    // :: warning: (cast.unsafe.constructor.invocation)
    new @LocalizableKey Issue2163FinalAA();
    // :: warning: (cast.unsafe.constructor.invocation)
    new @LocalizableKeyBottom Issue2163FinalAA();
  }
}

// Testing Default
@SuppressWarnings("anno.on.irrelevant")
class Issue2163FinalBCClient {
  @LocalizableKeyBottom Issue2163FinalBC obj = new Issue2163FinalBC();
}
