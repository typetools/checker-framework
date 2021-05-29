import java.util.Map;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AssumeKeyForTest {

  void m1(Map<String, Integer> m, String k) {
    @NonNull Integer x = m.get(k);
  }

  void m2(Map<String, Integer> m, String k) {
    @Nullable Integer x = m.get(k);
  }

  void m3(Map<String, @Nullable Integer> m, String k) {
    // :: error: (assignment)
    @NonNull Integer x = m.get(k);
  }

  void m4(Map<String, @Nullable Integer> m, String k) {
    @Nullable Integer x = m.get(k);
  }

  void m5(Map<String, Integer> m, @KeyFor("#1") String k) {
    @NonNull Integer x = m.get(k);
  }

  void m6(Map<String, Integer> m, @KeyFor("#1") String k) {
    @Nullable Integer x = m.get(k);
  }

  void m7(Map<String, @Nullable Integer> m, @KeyFor("#1") String k) {
    // :: error: (assignment)
    @NonNull Integer x = m.get(k);
  }

  void m8(Map<String, @Nullable Integer> m, @KeyFor("#1") String k) {
    @Nullable Integer x = m.get(k);
  }
}
