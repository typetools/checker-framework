// Test case for Issue 1500:
// https://github.com/typetools/checker-framework/issues/1500

import java.io.InputStream;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TryFinally2 {

  @SuppressWarnings("nullness") // dummy implementation
  Process getProcess() {
    return null;
  }

  void performCommand() {
    Process proc = null;
    InputStream in = null;
    try {
      proc = getProcess();
      in = proc.getInputStream();
      return;
    } finally {
      closeQuietly(in);
      if (proc != null) {
        proc.destroy();
      }
    }
  }

  public static void closeQuietly(final @Nullable InputStream input) {}
}
