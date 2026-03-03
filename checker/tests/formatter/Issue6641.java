import java.util.Map;

public class Issue6641 {
  static void foo(Map<String, String> map) {}

  public static void main(String[] args) {
    var input = Map.of("key1", "value1");
    foo(input);
  }
}
