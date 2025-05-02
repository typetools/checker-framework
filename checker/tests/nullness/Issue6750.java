package open.falsepos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.KeyFor;

// @below-java17-jdk-skip-test
public record Issue6750(String type) {

  void needKeyFor(@KeyFor("#2") String s, Map<String, String> map) {
    throw new RuntimeException();
  }

  @KeyFor("#1") String returnKeyFor(Map<String, String> map) {
    throw new RuntimeException();
  }

  Map<String, String> getMap(Function<String, String> s) {
    throw new RuntimeException();
  }

  void use() {
    // :: error: (argument)
    needKeyFor("", getMap(String::toString));
    // :: error: (expression.unparsable) :: error: (assignment)
    @KeyFor("getMap(String::toString)") String s = returnKeyFor(new HashMap<>(getMap(String::toString)));
  }

  void method(List<Issue6750> externals) {
    externals.stream().collect(Collectors.groupingBy(Issue6750::type)).entrySet().stream()
        .forEach(
            values -> {
              // :: error: (assignment)
              @KeyFor({}) String b = values.getKey();
            });
  }

  void test(List<Issue6750> externals) {
    externals.stream().collect(Collectors.groupingBy(Issue6750::type)).entrySet().stream()
        .forEach(
            values -> {
              throw new RuntimeException("");
            });
  }
}
