import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.i18n.qual.Localized;

public class I18nCollectorsToList {

  void m(List<String> strings) {
    Stream<String> s = strings.stream();

    List<String> collectedStrings1 = s.collect(Collectors.<String>toList());
    List<String> collectedStrings = s.collect(Collectors.toList());

    // :: error: (methodref.param)
    collectedStrings.forEach(System.out::println);
  }

  void m2(List<@Localized String> strings) {
    Stream<@Localized String> s = strings.stream();

    List<@Localized String> collectedStrings = s.collect(Collectors.toList());

    collectedStrings.forEach(System.out::println);
  }
}
