import java.util.List;

public abstract class Issue2565 {

  // Broken Case:
  abstract void processErrors(List<Error<? extends Enum<?>>> errors);

  static class Error<T extends Enum<T> & Hoo> {}

  interface Hoo {}
}
