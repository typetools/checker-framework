import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.*;

public final class StaticInLoop {

  public static @MonotonicNonNull String data_trace_state = null;

  @RequiresNonNull("StaticInLoop.data_trace_state")
  private static void read_vals_and_mods_from_trace_file(Object[] vals, int[] mods) {
    for (; ; ) {
      data_trace_state.toString();
      vals[0] = "hello";
    }
  }
}
