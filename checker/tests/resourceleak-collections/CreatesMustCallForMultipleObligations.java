// A call to a `@CreatesMustCallFor` method whose target is a resource collection whose elements
// have several must-call methods.  The collection has one collection obligation per must-call
// method of its elements, and the call resets all of them.

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.mustcall.qual.*;

class CreatesMustCallForMultipleObligations {

  // The Resource Leak Checker determines whether a `@CreatesMustCallFor` target has must-call
  // methods from the declaration of its class, not from its `@MustCall` annotation.
  @CreatesMustCallFor("#1")
  // :: error: creates.mustcall.for.invalid.target
  static void reset(@MustCall("shutdown") List<Resource> list) {}

  void resetThenDispose() {
    List<Resource> list = new ArrayList<>();
    list.add(new Resource());
    reset(list);
    for (Resource r : list) {
      r.close();
      r.flush();
    }
  }
}
