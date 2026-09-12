/*
 * @test
 * @summary Type-checking-performance regression test for the cost of
 * AnnotatedTypeMirror.hashCode(), issue #7023:
 * https://github.com/typetools/checker-framework/issues/7023
 * One Map.ofEntries call with 25 nested Map.entry arguments.  Invocation type inference explores
 * a large number of type-argument combinations for it, and each one probes SubtypeVisitHistory
 * and StructuralEqualityVisitHistory, which are keyed on a pair of AnnotatedTypeMirrors.  When
 * AnnotatedTypeMirror.hashCode() was a recursive walk of the whole type -- and, worse, a
 * structural walk while equals() compares underlying types with Type.equals(), which is reference
 * equality for every javac type but ArrayType -- every probe cost two full traversals and the
 * colliding, never-equal entries turned each bucket lookup into a long scan.  The Nullness Checker then reported 18-31 seconds per qualifier hierarchy on this file.
 *
 * There is nothing to type-check here; the file is expected to produce no diagnostics at all.
 *
 * -AslowTypecheckingSeconds is set low enough that a performance regression produces a
 * "slow.typechecking" warning, and -Werror turns that warning into a test failure.  Measured
 * through jtreg on an otherwise idle machine: 11 seconds, versus the 18-31 seconds reported
 * without the fix this test guards.  That is a narrower margin than the other tests here, so
 * re-measure before changing this threshold.
 *
 * @compile/timeout=600 -Werror -processor org.checkerframework.checker.nullness.NullnessChecker -AslowTypecheckingSeconds=15 Issue7023.java
 */
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
