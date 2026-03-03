package open.falsepos;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Issue6629 {

  void method(Stream<String> stream) {
    stream
        .map(
            x -> {
              Object o = new Object();
              return new Other(o);
            })
        .collect(Collectors.toList());
  }

  static class Other {

    public Other(Object o) {}
  }
}
