// Test case for Issue 1708
// https://github.com/typetools/checker-framework/issues/1708

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings({
  "unchecked",
  "ainfertest",
  "value"
}) // Don't issue warnings during ainfer tests, because more than one round of inference is required
// for the value checker
public class Issue1708 {

  static class A<T extends B> {}

  static class B<T1, T2> {}

  static class C {}

  static class D<T1 extends E, T2 extends F> {}

  static class E<T1, T2 extends D> {}

  static class F {}

  static class B1 extends B<C, D> {}

  static class B2 extends B<C, D> {}

  static class B3 extends B<C, D> {}

  public static class Example extends A<B<C, D>> {
    private final List<B<C, D>> f;

    public Example(B1 b1, B2 b2, B3 b3) {
      f = ListUtil.of(b1, b2, b3);
    }
  }

  public static class ListUtil {
    public static <T> List<T> of(T... elems) {
      return new ArrayList<>(Arrays.asList(elems));
    }
  }
}
