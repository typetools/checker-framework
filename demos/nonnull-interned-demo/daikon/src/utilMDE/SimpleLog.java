package utilMDE;

import java.util.Stack;

public final class SimpleLog {

  public String indent_str = "";
  public boolean enabled;
  public boolean line_oriented = true;

  public static class LongVal {
    public long val;
    public LongVal (long val) {
      this.val = val;
    }
  }

  public Stack<LongVal> start_times = new Stack<LongVal>();

  public SimpleLog (boolean enabled) {
    this.enabled = enabled;
    start_times.push (new LongVal(System.currentTimeMillis()));
  }

  public SimpleLog() {
    this (true);
  }

  public final boolean enabled() {
    return enabled;
  }

  public final void indent() {
    if (enabled) {
      indent_str += "  ";
      start_times.push (new LongVal(System.currentTimeMillis()));
    }
  }

  public final void indent (String format, Object... args) {
    if (enabled) {
      log (format, args);
      indent();
    }
  }

  public final void exdent() {
    if (enabled) {
      indent_str = indent_str.substring (0, indent_str.length()-2);
      start_times.pop();
    }
  }

  public final void exdent (String format, Object... args) {
    if (enabled) {
      exdent();
      log (format, args);
    }
  }

  public final void exdent_time (String format, Object... args) {
    if (enabled) {
      exdent();
      log_time (format, args);
    }
  }

  public final void log (String format, Object... args) {

    if (enabled) {
      System.out.print (indent_str);
      format = fix_format(format);
      System.out.printf (format, args);
    }

  }

  private final String fix_format (String format) {

    if (!line_oriented)
      return format;

    if (format.endsWith ("%n"))
      return format;

    return format + "%n";
  }

  public final void start_time() {
    if (enabled)
      start_times.peek().val = System.currentTimeMillis();
  }

  /**
   * Writes the specified message and the elapsed time since
   * the last call to start_time().  Calls start_time() after
   * printing message
   */
  public final void log_time (String format, Object... args) {

    if (enabled) {
      long elapsed = System.currentTimeMillis() - start_times.peek().val;
      System.out.print (indent_str);
      if (elapsed > 1000)
        System.out.printf ("[%,f secs] ", elapsed/1000.0);
      else
        System.out.print ("[" + elapsed + " ms] ");
      format = fix_format(format);
      System.out.printf (format, args);
      // start_time();
    }
  }
}
