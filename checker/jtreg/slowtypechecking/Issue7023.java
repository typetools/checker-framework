/*
 * @test
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
