// Test case for Issue 1506
// https://github.com/typetools/checker-framework/issues/1506

import java.io.IOException;
import java.util.ArrayList;

@SuppressWarnings("all") // just check for crashes.
public class Issue1506 {
  static void m() {
    ArrayList<? super Exception> l = new ArrayList<>();
    try {
      throw new IOException();
    } catch (RuntimeException | IOException e) {
      l.add(e);
    }
  }
}
