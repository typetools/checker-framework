package open.crash;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings("all") // Just check for crashes.
public class Issue7019 {

  OtherList<Entry> children;

  private Branches<RSnapshot> method(Entry entry) throws GitCoreException {
    final var childBranchTries = children.map(c -> Try.of(() -> method(c)));

    final var newRoots =
        Try.sequence(childBranchTries).getOrElseThrow(GitCoreException::getOrWrap).getT();
    throw new RuntimeException();
  }

  interface OtherList<T> {

    <U> List<U> map(Function<T, U> mapper);
  }

  public interface CheckedFunction0<R> {

    R apply() throws Throwable;
  }

  static class BSnapshot {}

  static class Entry {}

  static class RSnapshot extends BSnapshot {}

  static class Try<T> {

    static <T> Try<Seq<T>> sequence(Iterable<? extends Try<? extends T>> values) {
      throw new RuntimeException();
    }

    static <T> Try<T> of(CheckedFunction0<? extends T> supplier) {
      throw new RuntimeException();
    }

    <X extends Throwable> T getOrElseThrow(Function<? super Throwable, X> exceptionProvider)
        throws X {
      throw new RuntimeException();
    }
  }

  static class Seq<T> {
    T getT() {
      throw new RuntimeException();
    }

    T fold(T zero, BiFunction<? super T, ? super T, ? extends T> combine) {
      throw new RuntimeException();
    }
  }

  static class GitCoreException extends Exception {

    public static GitCoreException getOrWrap(Throwable e) {
      throw new RuntimeException();
    }
  }

  static final class Branches<T extends BSnapshot> {}
}
