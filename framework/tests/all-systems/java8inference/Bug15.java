package inference;

import java.util.Map.Entry;

@SuppressWarnings("all") // check for crashes
public class Bug15<B> {
  public void putAll(Entry<? extends Class<? extends B>, B> entry) {
    cast(entry.getKey(), entry.getValue());
  }

  private static <F, T extends F> T cast(Class<T> type, F value) {
    throw new RuntimeException();
  }
}
