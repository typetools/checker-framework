// The Nullness Checker has two qualifier hierarchies: the nullness hierarchy and the
// initialization hierarchy.  This test checks that WPI infers an annotation in every hierarchy
// for a use of a type variable that is explicitly annotated in only one of them.  Here, the
// return type is explicitly annotated in the initialization hierarchy, so WPI must still infer
// @Nullable for it in the nullness hierarchy.

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TypeVarMultipleHierarchies<T extends @NonNull Object> {

  public @UnknownInitialization T identity(@UnknownInitialization @Nullable T x) {
    // :: warning: [return]
    return x;
  }
}
