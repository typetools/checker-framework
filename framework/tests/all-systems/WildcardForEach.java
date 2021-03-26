import java.util.List;

public class WildcardForEach {
  static class Gen<T extends Gen<?>> {}

  void test(List<Gen<?>> x) {
    // This used to cause a crash in
    // org.checkerframework.framework.flow.CFTreeBuilder#buildAnnotatedType
    for (Gen<?> a : x) {}
  }
}
