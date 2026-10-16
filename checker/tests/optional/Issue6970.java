import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public class Issue6970 {

  public static Path getPath(String moduleName) {
    final String fileNamePattern = ".*[\\\\/]" + moduleName.toLowerCase(Locale.ROOT) + "\\..*";
    return getPaths().stream()
        .filter(path -> path.toString().matches(fileNamePattern))
        .findFirst()
        .orElse(null);
  }

  private static Set<Path> getPaths() {
    throw new RuntimeException("");
  }
}
