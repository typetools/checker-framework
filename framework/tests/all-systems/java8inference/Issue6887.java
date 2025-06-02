// https://github.com/typetools/checker-framework/issues/6887

import java.io.IOException;

public final class Issue6887<E extends Exception> {

  public Issue6887(ThrowingFunctionalInterface<E> throwingFunctionalInterface) {}

  public static void main(String[] args) throws IOException {
    new Issue6887<IOException>(
        () -> {
          throw new IOException("");
        });
    new Issue6887<>(
        () -> {
          throw new IOException("");
        });
    new Issue6887<RuntimeException>(() -> {});
    new Issue6887<>(() -> {});
  }

  @FunctionalInterface
  public interface ThrowingFunctionalInterface<E extends Exception> {
    void throwingFunction() throws E;
  }
}
