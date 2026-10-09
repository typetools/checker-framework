// Test case for https://github.com/typetools/checker-framework/issues/294

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public abstract class Issue294 {
  public @MonotonicNonNull String header;

  public void f() {
    if (header == null) {
      write("null");
    } else {
      runWithRetry(
          new Runnable() {
            @Override
            public void run() {
              write(header);
            }
          });
    }
  }

  public void g() {
    runWithRetry(
        new Runnable() {
          @Override
          public void run() {
            // :: error: [argument]
            write(header);
          }
        });
  }

  public abstract void write(String s);

  public abstract void runWithRetry(Runnable r);
}
