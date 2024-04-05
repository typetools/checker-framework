import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("all") // Just check for crashes.
public class Quarkus2 {
  private void method(
      CompletableFuture<InferfaceA> server,
      CompletableFuture<List<ClassD<ClassB, ClassC>>> list,
      CompletableFuture<List<ClassD<ClassB, ClassC>>> x) {
    CompletableFuture<List<ClassD<ClassB, ClassC>>> q =
        server.thenCompose(
            ls -> {
              x.thenApply(
                  y -> {
                    return y;
                  });
              return list;
            });
  }

  public interface InferfaceA {}

  static class ClassB {}

  static class ClassC {}

  public static class ClassD<L, R> {}
}
