import java.util.Iterator;
import org.checkerframework.dataflow.qual.TerminatesExecution;
import org.checkerframework.framework.test.*;
import org.checkerframework.framework.testchecker.util.*;

// various tests for @TerminatesExecution
public class Termination {

  @TerminatesExecution
  void exit() {}

  void t1(@Odd String p1, String p2, boolean b1) {
    String l1 = p2;
    if (b1) {
      l1 = p1;
    } else {
      exit();
    }
    @Odd String l3 = l1;
  }

  static class TerminatingCloseable implements AutoCloseable {
    @Override
    @TerminatesExecution
    public void close() {
      throw new Error();
    }
  }

  // The desugared call to close() does not unconditionally terminate execution, because a
  // try-with-resources statement does not call close() if the resource is null.
  void t2(@Odd String p1, String p2, boolean b1, TerminatingCloseable c) {
    String l1 = p2;
    if (b1) {
      l1 = p1;
    } else {
      try (c) {}
    }
    // :: error: [assignment]
    @Odd String l3 = l1;
  }

  // The desugared call to close() terminates execution, because the resource is not null.
  void t3(@Odd String p1, String p2, boolean b1) {
    String l1 = p2;
    if (b1) {
      l1 = p1;
    } else {
      try (TerminatingCloseable c = new TerminatingCloseable()) {}
    }
    @Odd String l3 = l1;
  }

  static class ReturningCloseable implements AutoCloseable {
    @Override
    public void close() {}
  }

  // The desugared call to close() returns normally.
  void t4(@Odd String p1, String p2, boolean b1) {
    String l1 = p2;
    if (b1) {
      l1 = p1;
    } else {
      try (ReturningCloseable c = new ReturningCloseable()) {}
    }
    // :: error: [assignment]
    @Odd String l3 = l1;
  }

  static class CheckedException extends Exception {}

  static void throwChecked() throws CheckedException {}

  // The desugared call to close() on the path for a break statement terminates execution, so the
  // loop never exits.
  void t5(@Odd String p1, String p2, boolean b1, boolean b2) {
    String l1 = p2;
    if (b1) {
      l1 = p1;
    } else {
      while (true) {
        try (TerminatingCloseable c = new TerminatingCloseable()) {
          if (b2) {
            break;
          }
        }
      }
    }
    @Odd String l3 = l1;
  }

  // The desugared call to close() terminates execution, but on the exceptional path, an exception
  // that close() throws is suppressed and the primary exception propagates, so the catch clause is
  // reachable.
  void t6(@Odd String p1, String p2, boolean b1) {
    String l1 = p2;
    if (b1) {
      l1 = p1;
    } else {
      try {
        try (TerminatingCloseable c = new TerminatingCloseable()) {
          throwChecked();
        }
      } catch (CheckedException e) {
      }
    }
    // :: error: [assignment]
    @Odd String l3 = l1;
  }

  // Like t6, but the desugared call to close() returns normally.
  void t6Returning(@Odd String p1, String p2, boolean b1) {
    String l1 = p2;
    if (b1) {
      l1 = p1;
    } else {
      try {
        try (ReturningCloseable c = new ReturningCloseable()) {
          throwChecked();
        }
      } catch (CheckedException e) {
      }
    }
    // :: error: [assignment]
    @Odd String l3 = l1;
  }

  static class TerminatingIterable implements Iterable<Object> {
    @Override
    @TerminatesExecution
    public Iterator<Object> iterator() {
      throw new Error();
    }
  }

  // The desugared call to iterator() terminates execution.
  void t7(@Odd String p1, String p2, boolean b1) {
    String l1 = p2;
    if (b1) {
      l1 = p1;
    } else {
      for (Object o : new TerminatingIterable()) {}
    }
    @Odd String l3 = l1;
  }

  // The desugared call to iterator() on a parameter terminates execution.
  void t8(@Odd String p1, String p2, boolean b1, TerminatingIterable it) {
    String l1 = p2;
    if (b1) {
      l1 = p1;
    } else {
      for (Object o : it) {}
    }
    @Odd String l3 = l1;
  }

  // The desugared call to iterator() can throw an exception that a catch clause handles.
  void t9(@Odd String p1, String p2, boolean b1, TerminatingIterable it) {
    String l1 = p2;
    if (b1) {
      l1 = p1;
    } else {
      try {
        for (Object o : it) {}
      } catch (Error e) {
      }
    }
    // :: error: [assignment]
    @Odd String l3 = l1;
  }

  static class TerminatingConstructor {
    @TerminatesExecution
    TerminatingConstructor() {
      throw new Error();
    }
  }

  // The constructor invocation terminates execution.
  void t10(@Odd String p1, String p2, boolean b1) {
    String l1 = p2;
    if (b1) {
      l1 = p1;
    } else {
      new TerminatingConstructor();
    }
    @Odd String l3 = l1;
  }
}
