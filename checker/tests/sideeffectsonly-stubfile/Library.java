package sideeffectsonly.stubfile;

// The `@SideEffectsOnly` annotations on these methods come from `seonly.astub`.
public class Library {
  // The annotation is `@SideEffectsOnly("this.noSuchField")`, which names a field that does not
  // exist.  The declaration-site check reports that.
  // :: error: (flowexpr.parse.error)
  public void unparseable() {}

  public void parseable() {}

  // `seonly.astub` declares this method `@SideEffectFree`.  Both annotations are written on the
  // declaration -- an annotation file is as much a declaration as source code is -- so this is a
  // conflict, just as if both appeared in this file.
  @org.checkerframework.dataflow.qual.SideEffectsOnly("this")
  // :: error: (purity.incorrect.annotation.conflict)
  public void conflictsWithStub() {}
}

interface Callback {
  // `seonly.astub` declares this method `@SideEffectsOnly("this.noSuchField")`.
  // :: error: (flowexpr.parse.error)
  void run();
}
