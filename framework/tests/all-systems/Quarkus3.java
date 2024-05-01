import java.util.List;
import java.util.stream.Collectors;

public class Quarkus3 {
  void method(List<String> commands) {
    String text =
        commands.stream()
            .map(
                param -> {
                  if (param.indexOf(' ') != -1) {
                    return "\"" + param + "\"";
                  }
                  return param;
                })
            .collect(Collectors.joining(" "));
  }
}
