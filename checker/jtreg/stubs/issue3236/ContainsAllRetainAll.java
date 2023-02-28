/*
 * @test
 * @summary Test case for Issue 3236 https://github.com/typetools/checker-framework/issues/3236
 *
 * @compile -XDrawDiagnostics  -Anomsgtext -processor org.checkerframework.checker.nullness.NullnessChecker  ContainsAllRetainAll.java -Aignorejdkastub
 * @compile/fail/ref=NoJdkExplicitStub.out  -XDrawDiagnostics  -Anomsgtext -processor org.checkerframework.checker.nullness.NullnessChecker  ContainsAllRetainAll.java -Aignorejdkastub -Astubs=nonnull-collection.astub
 * @compile  -XDrawDiagnostics  -Anomsgtext -processor org.checkerframework.checker.nullness.NullnessChecker  ContainsAllRetainAll.java -Aignorejdkastub -Astubs=collection-object-parameters-may-be-null.astub
 * @compile  -XDrawDiagnostics  -Anomsgtext -processor org.checkerframework.checker.nullness.NullnessChecker  ContainsAllRetainAll.java -Astubs=collection-object-parameters-may-be-null.astub
 *
 *  @compile/fail/ref=JdkNoStub.out -XDrawDiagnostics  -Anomsgtext -processor org.checkerframework.checker.nullness.NullnessChecker  ContainsAllRetainAll.java
 */

import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;

class ContainsAllRetainAll {
  void bulkOperations(Collection<String> a, Collection<@Nullable String> b) {
    a.containsAll(b);
  }
}
