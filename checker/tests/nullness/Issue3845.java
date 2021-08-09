package wildcards;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

public class Issue3845 {
  static class Holder<T> {
    final T value;

    Holder(T value) {
      this.value = value;
    }
  }

  interface HolderSupplier<H extends Holder<Object>> {
    H get();
  }

  @DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.ALL)
  static class DefaultClash {

    Object go(HolderSupplier<?> s) {
      if (s != null) {
        return s.get();
      }
      return "";
    }
  }
}
