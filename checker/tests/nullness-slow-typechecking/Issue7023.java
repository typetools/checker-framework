// Regression test for the cost of AnnotatedTypeMirror.hashCode().
//
// One Map.ofEntries call with 25 nested Map.entry arguments.  Invocation type inference
// explores a large number of type-argument combinations for it, and each one probes
// SubtypeVisitHistory and StructuralEqualityVisitHistory, which are keyed on a pair of
// AnnotatedTypeMirrors.  When AnnotatedTypeMirror.hashCode() was a recursive walk of the whole
// type -- and, worse, a structural walk while equals() compares underlying types by reference --
// every probe cost two full traversals and the colliding, never-equal entries turned each bucket
// lookup into a long scan.  The Nullness Checker reported 18-31 seconds per qualifier hierarchy
// on this file.
//
// There is nothing to type-check here; the file is expected to produce no diagnostics at all.

import java.util.Map;

public class Issue7023 {
  private static final Map<Class<Throwable>, Map.Entry<Integer, String>> testOutput =
      Map.ofEntries(
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")),
          Map.entry(Throwable.class, Map.entry(0, "")));
}
