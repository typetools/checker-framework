import java.util.*;
import org.checkerframework.checker.nullness.qual.*;

class A {
  <T extends List<Integer>> T m(T x) {
    return x;
  }
}

class B extends A {
  <T extends List<@Nullable Integer>> T m(T x) {
    x.add(null);
    return x;
  }
}

public class Issue6864 {
  public static void main(String[] args) {
    A x = new B();
    List<Integer> y = new LinkedList<>();
    x.m(y).get(0).toString();
  }
}
