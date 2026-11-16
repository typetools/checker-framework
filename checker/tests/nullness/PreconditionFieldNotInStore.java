// This test checks that a precondition can use the declared type
// of a field when it isn't in the store. Based on a false positive
// encountered in BCELUtil while testing WPI.

import java.io.PrintStream;

class PreconditionFieldNotInStore {

  private @org.checkerframework.checker.nullness.qual.MonotonicNonNull String filename;

  @org.checkerframework.framework.qual.RequiresQualifier(
      expression = {"this.filename"},
      qualifier = org.checkerframework.checker.nullness.qual.Nullable.class)
  @org.checkerframework.checker.nullness.qual.NonNull String getIndentString() {
    return "indentString";
  }

  public void logStackTrace(PrintStream logfile, int[] ste_arr) {
    for (int ii = 2; ii < ste_arr.length; ii++) {
      int ste = ste_arr[ii];
      logfile.printf("%s  %s%n", getIndentString(), ste);
    }
    logfile.flush();
  }
}
