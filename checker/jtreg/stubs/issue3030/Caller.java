import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;

/*
 * @test
 * @summary Test case for Issue 3030 https://github.com/typetools/checker-framework/issues/3030
 *
 * @compile/fail/ref=Caller.out -XDrawDiagnostics  -Anomsgtext -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=ConcurrentHashMap.astub  Caller.java
 */
class Caller {
  void foo() {
    ConcurrentHashMap<@Nullable String, @Nullable String> m;
  }
}
