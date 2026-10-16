package open.falsepos;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue6740 {

  static <A> Optional<@NonNull A> ofNullable(@Nullable A value) {
    throw new RuntimeException();
  }

  <B> @Nullable B getNullable(Class<B> type) {
    throw new RuntimeException();
  }

  <C> Optional<@NonNull C> getOpt(int index, Class<C> type) {
    return ofNullable(getNullable(type));
  }
}
