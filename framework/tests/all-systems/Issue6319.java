import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

class Issue6319 {
  void f(List<? extends Enum<?>> list) {
    for (Enum<?> value :
        list.stream().sorted(Comparator.comparing(Enum::name)).collect(Collectors.toList())) {}
  }
}
