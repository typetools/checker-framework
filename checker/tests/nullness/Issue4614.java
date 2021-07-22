import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class Issue4614 {

    public static Map<String, String> getAllVersionInformation() {
        return new HashMap<>();
    }

    public void method1() {
        final String versionInfo =
                Issue4614.getAllVersionInformation().entrySet().stream() //
                        .map(e -> String.format("%s:%s", e.getKey(), e.getValue())) //
                        .collect(Collectors.joining("\n"));
    }

    Map<String, String> allVersionInformation = new HashMap<>();

    public void method2() {
        final String versionInfo =
                allVersionInformation.entrySet().stream() //
                        .map(e -> String.format("%s:%s", e.getKey(), e.getValue())) //
                        .collect(Collectors.joining("\n"));
    }
}
