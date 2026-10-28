public class Test {

  public sealed interface Shape {}

  public record Circle(int radius) implements Shape {}

  public record Square(int side) implements Shape {}

  public int test(Shape shape) {
    int size = 0;
    switch (shape) {
      case Circle(int radius) -> {
        size = radius;
      }
      case Square(int side) -> {
        size = side;
      }
    }
    return size;
  }
}
