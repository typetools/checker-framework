package fakeoverrides;

import java.util.List;

public class DefineClasses {}

interface SuperInterface {
  default int m() {
    return 0;
  }

  default int g(List<String> l) {
    return 0;
  }
}

class SuperClass implements SuperInterface {
  // fake override:
  // @Untainted int m();
  // @Untainted int g(List<String> l);
}

interface SubInterface extends SuperInterface {
  // fake override:
  // int m();
}
