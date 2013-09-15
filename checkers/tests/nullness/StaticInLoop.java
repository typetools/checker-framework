import checkers.nullness.quals.*;
import dataflow.quals.*;

// @skip-tests TEMPORARY -- this class indicates a bug in the Checker Framework!
public final class StaticInLoop {

  public static /*@MonotonicNonNull*/ ParseState data_trace_state = null;

  /*@RequiresNonNull("StaticInLoop.data_trace_state")*/
  private static void read_vals_and_mods_from_trace_file(Object[] vals, int[] mods) {
    // If you remove the for loop (retaining the body), the problem disappears
    for ( ; ; ) {

      // ***** HERE IS THE PROBLEM *****
      System.out.println(data_trace_state.filename);

      // If you comment out this line, the problem disappears
      vals[0] = "hello";
    }

  }

  public static class ParseState {
    public String filename = "some filename";
  }

}
