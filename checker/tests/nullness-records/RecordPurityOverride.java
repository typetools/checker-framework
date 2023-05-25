import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

record RecordPurityOverride(@Nullable String pure, @Nullable String impure) {
  @Pure
  public @Nullable String pure() {
    return pure;
  }

  // Note: not @Pure
  public @Nullable String impure() {
    return impure;
  }

  public String checkPurityOfFields() {
    if (pure == null || impure == null) return "";
    else return pure.toString() + " " + impure.toString();
  }

  public String checkPurityOfAccessor1() {
    if (pure() == null || impure() == null) return "";
    else
      // :: error: (dereference.of.nullable)
      return pure().toString() + " " + impure().toString();
  }

  public String checkPurityOfAccessor2() {
    if (pure() == null) return "";
    else return pure().toString();
  }

  public String checkPurityOfAccessor3() {
    if (impure() == null) return "";
    else
      // :: error: (dereference.of.nullable)
      return impure().toString();
  }
}
