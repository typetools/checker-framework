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

  static class TerminatingIterable implements Iterable<String> {
    @Override
    @TerminatesExecution
    public Iterator<String> iterator() {
      throw new Error();
    }
  }

  // The desugared call to iterator() terminates execution.
  void t2(@Odd String p1, String p2, boolean b1, TerminatingIterable it) {
    String l1 = p2;
    if (b1) {
      l1 = p1;
    } else {
      for (String s : it) {}
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

  // The desugared call to close() does not terminate execution, because a try-with-resources
  // statement does not call close() if the resource is null.
  void t3(@Odd String p1, String p2, boolean b1, TerminatingCloseable c) {
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
  void t4(@Odd String p1, String p2, boolean b1) {
    String l1 = p2;
    if (b1) {
      l1 = p1;
    } else {
      try (TerminatingCloseable c = new TerminatingCloseable()) {}
    }
    @Odd String l3 = l1;
  }
}
