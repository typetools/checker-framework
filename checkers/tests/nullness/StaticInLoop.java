import checkers.nullness.quals.*;
import dataflow.quals.*;

public final class StaticInLoop {

  public static @MonotonicNonNull String data_trace_state = null;

  @RequiresNonNull("StaticInLoop.data_trace_state")
  private static void read_vals_and_mods_from_trace_file(Object[] vals, int[] mods) {
    // If you remove the for loop (retaining the body), the problem disappears
    for ( ; ; ) {

      // ***** HERE IS THE PROBLEM *****
      data_trace_state.toString();

      // If you comment out this line, the problem disappears
      vals[0] = "hello";
    }

  }

}
