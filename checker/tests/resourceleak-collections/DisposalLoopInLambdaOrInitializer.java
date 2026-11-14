// A disposal loop in a lambda or an initializer block is a disposal loop, just as in a method.

import java.util.ArrayList;
import java.util.List;

class DisposalLoopInLambdaOrInitializer {

  {
    List<Resource> l = new ArrayList<>();
    l.add(new Resource());
    for (Resource r : l) {
      r.close();
      r.flush();
    }
  }

  static {
    List<Resource> l = new ArrayList<>();
    l.add(new Resource());
    for (Resource r : l) {
      r.close();
      r.flush();
    }
  }

  void lambda() {
    Runnable run =
        () -> {
          List<Resource> l = new ArrayList<>();
          l.add(new Resource());
          for (Resource r : l) {
            r.close();
            r.flush();
          }
        };
  }
}
