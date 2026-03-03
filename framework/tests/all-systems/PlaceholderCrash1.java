import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@SuppressWarnings("all") // Just check for crashes.
public class PlaceholderCrash1 {
  void method(PlaceholderAPIPlugin plugin) {
    final List<CloudExpansion> expansions = new ArrayList<>();
    onMainThread(
        plugin,
        downloadAndDiscover(expansions, plugin),
        (classes, exception) -> {
          if (exception != null) {
            return;
          }
          final String message =
              classes.stream()
                  .filter(Objects::nonNull)
                  .map(plugin.getLocalExpansionManager()::register)
                  .filter(Optional::isPresent)
                  .map(Optional::get)
                  .map(expansion -> "  &a" + expansion.getName())
                  .collect(Collectors.joining("\n"));
          return;
        });
  }

  public static <T> void onMainThread(
      final Plugin plugin,
      final CompletableFuture<T> future,
      final BiConsumer<T, Throwable> consumer) {}

  private static CompletableFuture<List<Class<? extends PlaceholderExpansion>>> downloadAndDiscover(
      final List<CloudExpansion> expansions, final PlaceholderAPIPlugin plugin) {
    throw new RuntimeException();
  }

  static class Plugin {}

  static class PlaceholderAPIPlugin extends Plugin {
    LocalExpansionManager getLocalExpansionManager() {
      throw new RuntimeException();
    }
  }

  static class LocalExpansionManager {
    Optional<PlaceholderExpansion> register(Class<? extends PlaceholderExpansion> p) {
      throw new RuntimeException();
    }
  }

  static class PlaceholderExpansion {

    public String getName() {
      return "";
    }
  }

  static class CloudExpansion {}
}
