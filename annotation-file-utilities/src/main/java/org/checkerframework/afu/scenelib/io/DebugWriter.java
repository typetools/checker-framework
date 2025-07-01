package org.checkerframework.afu.scenelib.io;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Performs output to System.out. Has a flag {@code enabled} that controls whether output is
 * performed.
 */
public class DebugWriter {
  /** A PrintWriter for System.out. */
  private PrintWriter out =
      new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out, UTF_8)));

  /** Whether this DebugWriter is enabled. */
  private boolean enabled;

  /**
   * Creates a new DebugWriter.
   *
   * @param enabled whether this DebugWriter is enabled
   */
  public DebugWriter(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns whether this DebugWriter is enabled.
   *
   * @return whether this DebugWriter is enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Set the enabled status of this DebugWriter.
   *
   * @param enabled whether this DebugWriter is enabled
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Print to System.out if this DebugWriter is enabled.
   *
   * @param format a format string
   * @param args the format string arguments
   */
  @com.google.errorprone.annotations.FormatMethod
  public void debug(String format, Object... args) {
    if (isEnabled()) {
      out.print(String.format(format, args));
      out.flush();
    }
  }
}
