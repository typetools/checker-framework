// A `while` disposal loop in a lambda or an initializer block is a disposal loop, just as in a
// method.

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class WhileDisposalLoopInLambdaOrInitializer {

  {
    List<Resource> l = new ArrayList<>();
    l.add(new Resource());
    Iterator<Resource> it = l.iterator();
    while (it.hasNext()) {
      Resource r = it.next();
      r.close();
      r.flush();
    }
  }

  static {
    List<Resource> l = new ArrayList<>();
    l.add(new Resource());
    Iterator<Resource> it = l.iterator();
    while (it.hasNext()) {
      Resource r = it.next();
      r.close();
      r.flush();
    }
  }

  void lambda() {
    Runnable run =
        () -> {
          List<Resource> l = new ArrayList<>();
          l.add(new Resource());
          Iterator<Resource> it = l.iterator();
          while (it.hasNext()) {
            Resource r = it.next();
            r.close();
            r.flush();
          }
        };
  }
}
