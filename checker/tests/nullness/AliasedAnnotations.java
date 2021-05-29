// This uses all the aliases listed in section "Other tools for nullness
// checking" of the Checker Framework manual.

public class AliasedAnnotations {

  void useNonNullAnnotations() {
    // :: error: (assignment)
    @org.checkerframework.checker.nullness.qual.NonNull Object nn1 = null;
    // :: error: (assignment)
    @com.sun.istack.internal.NotNull Object nn2 = null;
    // :: error: (assignment)
    @edu.umd.cs.findbugs.annotations.NonNull Object nn3 = null;
    // :: error: (assignment)
    @javax.annotation.Nonnull Object nn4 = null;
    // Invalid location for NonNull :: error: (assignment)
    // @javax.validation.constraints.NotNull Object nn5 = null;
    // :: error: (assignment)
    @org.eclipse.jdt.annotation.NonNull Object nn6 = null;
    // :: error: (assignment)
    @org.jetbrains.annotations.NotNull Object nn7 = null;
    // :: error: (assignment)
    @org.netbeans.api.annotations.common.NonNull Object nn8 = null;
    // :: error: (assignment)
    @org.jmlspecs.annotation.NonNull Object nn9 = null;
  }

  void useNullableAnnotations1(@org.checkerframework.checker.nullness.qual.Nullable Object nble) {
    // :: error: (dereference.of.nullable)
    nble.toString();
  }

  void useNullableAnnotations2(@com.sun.istack.internal.Nullable Object nble) {
    // :: error: (dereference.of.nullable)
    nble.toString();
  }

  void useNullableAnnotations3(@edu.umd.cs.findbugs.annotations.Nullable Object nble) {
    // :: error: (dereference.of.nullable)
    nble.toString();
  }

  void useNullableAnnotations4(@edu.umd.cs.findbugs.annotations.CheckForNull Object nble) {
    // :: error: (dereference.of.nullable)
    nble.toString();
  }

  void useNullableAnnotations5(@edu.umd.cs.findbugs.annotations.UnknownNullness Object nble) {
    // :: error: (dereference.of.nullable)
    nble.toString();
  }

  void useNullableAnnotations6(@javax.annotation.Nullable Object nble) {
    // :: error: (dereference.of.nullable)
    nble.toString();
  }

  void useNullableAnnotations7(@javax.annotation.CheckForNull Object nble) {
    // :: error: (dereference.of.nullable)
    nble.toString();
  }

  void useNullableAnnotations9(@org.eclipse.jdt.annotation.Nullable Object nble) {
    // :: error: (dereference.of.nullable)
    nble.toString();
  }

  void useNullableAnnotations10(@org.jetbrains.annotations.Nullable Object nble) {
    // :: error: (dereference.of.nullable)
    nble.toString();
  }

  void useNullableAnnotations12(@org.netbeans.api.annotations.common.NullAllowed Object nble) {
    // :: error: (dereference.of.nullable)
    nble.toString();
  }

  void useNullableAnnotations13(@org.netbeans.api.annotations.common.NullUnknown Object nble) {
    // :: error: (dereference.of.nullable)
    nble.toString();
  }

  void useNullableAnnotations14(@org.jmlspecs.annotation.Nullable Object nble) {
    // :: error: (dereference.of.nullable)
    nble.toString();
  }
}
