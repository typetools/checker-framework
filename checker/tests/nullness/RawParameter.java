// Test case for issue #516: https://github.com/typetools/checker-framework/issues/516

import org.checkerframework.checker.nullness.qual.Nullable;

public class RawParameter<T> {
  private @Nullable T payload;

  // Using a parameterized type here avoids the exception
  @SuppressWarnings("unchecked")
  public void clearPayload(RawParameter node) {
    node.payload = null;
  }
}
