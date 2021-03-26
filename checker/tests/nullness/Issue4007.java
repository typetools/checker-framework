// Test case for issue #4007: https://tinyurl.com/cfissue/4007

// @skip-test until the issue is fixed

import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;

final class Issue4007 {
  Optional<String> m1(List<String> list) {
    return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
  }

  Optional<Optional<String>> m2(List<String> list) {
    return Optional.of(list.isEmpty() ? Optional.empty() : Optional.of(list.get(0)));
  }

  Optional<Optional<String>> m3(List<String> list) {
    return Optional.of(
        list.isEmpty() ? Optional.<@NonNull String>empty() : Optional.of(list.get(0)));
  }

  Optional<Optional<String>> m4(List<String> list) {
    return Optional.of(
        list.isEmpty() ? Optional.empty() : Optional.<@NonNull String>of(list.get(0)));
  }

  Optional<Optional<String>> m5(List<String> list) {
    return Optional.of(
        list.isEmpty()
            ? Optional.<@NonNull String>empty()
            : Optional.<@NonNull String>of(list.get(0)));
  }
}
