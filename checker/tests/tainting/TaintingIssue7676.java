import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.Covariant;

public class TaintingIssue7676 {
  @Covariant(0)
  static class Optional767<T> {
    static <R> Optional767<R> of(R r) {
      throw new RuntimeException();
    }
  }

  Optional767<@Tainted Foo> repro(int i) {
    return Optional767.of(
        switch (i) {
          case 1 -> getBar();
          default -> getBar();
        });
  }

  @Untainted Bar getBar() {
    throw new RuntimeException();
  }

  static class Foo {}

  static class Bar extends Foo {}
}
