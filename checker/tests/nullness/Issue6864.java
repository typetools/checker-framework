import java.util.LinkedList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

class Issue6864_A {
  <T extends List<Integer>> T m(T x) {
    return x;
  }
}

class Issue6864_B extends Issue6864_A {
  <T extends List<@Nullable Integer>> T m(T x) {
    x.add(null);
    return x;
  }
}

public class Issue6864 {
  public static void main(String[] args) {
    Issue6864_A x = new Issue6864_B();
    List<Integer> y = new LinkedList<>();
    x.m(y).get(0).toString();
  }
}
