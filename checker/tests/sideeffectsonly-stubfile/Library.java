package sideeffectsonly.stubfile;

// The `@SideEffectsOnly` annotations on these methods come from `seonly.astub`.
public class Library {
  // The annotation is `@SideEffectsOnly("this.noSuchField")`, which names a field that does not
  // exist.  Two declaration-site checks detect that -- checking the annotation itself, and parsing
  // the expression in order to check the method body against it -- but they issue the same
  // message, so it is reported only once.
  // :: error: (flowexpr.parse.error.sideeffectsonly)
  public void unparseable() {}

  public void parseable() {}

  // `seonly.astub` declares this method `@SideEffectFree`.  Both annotations are written on the
  // declaration -- an annotation file is as much a declaration as source code is -- so this is a
  // conflict, just as if both appeared in this file.
  @org.checkerframework.dataflow.qual.SideEffectsOnly("this")
  // :: error: (purity.annotation.conflict)
  public void conflictsWithStub() {}
}

interface Callback {
  // `seonly.astub` declares this method `@SideEffectsOnly("this.noSuchField")`.
  // :: error: (flowexpr.parse.error.sideeffectsonly)
  void run();
}
