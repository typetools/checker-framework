package sideeffectsonly.stubfile;

// The `@SideEffectsOnly` annotations on these methods come from `seonly.astub`.
public class Library {
  // The annotation is `@SideEffectsOnly("this.noSuchField")`, which names a field that does not
  // exist.  The declaration-site check reports that.
  // :: error: (flowexpr.parse.error)
  public void unparseable() {}

  public void parseable() {}
}
