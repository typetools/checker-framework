import java.util.HashSet;
import java.util.Set;

public class ForEach {
  void m1() {
    Set<? extends CharSequence> s = new HashSet<CharSequence>();
    for (CharSequence cs : s) {
      cs.toString();
    }
  }

  void m2() {
    Set<CharSequence> s = new HashSet<>();
    for (CharSequence cs : s) {
      cs.toString();
    }
  }

  <T extends Object> void m3(T p) {
    Set<T> s = new HashSet<>();
    for (T cs : s) {
      cs.toString();
    }
  }

  <T extends Object> void m4(T p) {
    Set<T> s = new HashSet<>();
    for (Object cs : s) {
      cs.toString();
    }
  }
}
