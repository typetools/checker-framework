import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all") // Just check for crashes.
public class Issue6825 {
  static class ClassA<T extends Number> {}

  public static boolean flag;
  ClassA<? super Number> f;

  void method(Number n) {
    var y = flag ? f : new ClassA<Number>();
    var x = flag ? this.f : new ClassA<Number>();
  }

  static class SomeClass {}

  private List<? extends SomeClass> typeParameters = null;

  public Issue6825(Issue6825 other) {
    this.typeParameters =
        other.typeParameters == null ? null : new ArrayList<>(other.typeParameters);
  }
}
