import java.util.Optional;

abstract class Issue6438 {
  <T extends First & Second> Optional<T> a(boolean b, Class<T> clazz) {
    return b ? Optional.empty() : Optional.of(b(clazz));
  }

  abstract <T extends First & Second> T b(Class<T> clazz);

  interface First {}

  interface Second {}
}
