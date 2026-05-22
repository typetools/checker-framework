import org.checkerframework.framework.qual.Covariant;

public class Issue7676 {
  @Covariant(0)
  static class Optional767<T> {
    static <R> Optional767<R> of(R r) {
      throw new RuntimeException();
    }
  }

  Optional767<Foo> repro(int i) {
    return Optional767.of(
        switch (i) {
          case 1 -> new Bar();
          default -> new Bar();
        });
  }

  static class Foo {}

  static class Bar extends Foo {}
}
