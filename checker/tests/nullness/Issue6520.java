package open.falsepos;

import java.util.stream.Collectors;
import java.util.stream.Stream;

class Issue6520 {

  private record Data(String value) {}

  Issue6520(Stream<Data> data) {
    data.collect(Collectors.groupingBy(Data::value)).entrySet().stream()
        .filter(entry -> entry.getValue().size() > 1);
  }
}
