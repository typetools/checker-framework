import java.util.ArrayList;
import java.util.List;

/*
 * Tests that a disposal loop discharges the obligations of the collection it iterates over, and
 * no other collection's obligations.
 */
class DisposalLoopOwnCollectionOnly {

  void onlyFirstDisposed() {
    List<Resource> a = new ArrayList<>();
    a.add(new Resource());
    List<Resource> b = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    b.add(new Resource());
    for (Resource r : a) {
      r.close();
      r.flush();
    }
  }

  void onlySecondDisposed() {
    List<Resource> a = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    a.add(new Resource());
    List<Resource> b = new ArrayList<>();
    b.add(new Resource());
    for (Resource r : b) {
      r.close();
      r.flush();
    }
  }

  void bothDisposed() {
    List<Resource> a = new ArrayList<>();
    a.add(new Resource());
    List<Resource> b = new ArrayList<>();
    b.add(new Resource());
    for (Resource r : a) {
      r.close();
      r.flush();
    }
    for (Resource r : b) {
      r.close();
      r.flush();
    }
  }

  // The loop iterates over an alias of a.
  void aliasDisposed() {
    List<Resource> a = new ArrayList<>();
    a.add(new Resource());
    List<Resource> c = a;
    for (Resource r : c) {
      r.close();
      r.flush();
    }
  }

  // The loop calls no must-call method, so it discharges nothing.
  void loopCallsNothing() {
    List<Resource> a = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    a.add(new Resource());
    List<Resource> b = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    b.add(new Resource());
    for (Resource r : a) {
      r.toString();
    }
  }

  // a's loop misses flush; b's loop calls both methods.
  void firstPartiallyDisposed() {
    List<Resource> a = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    a.add(new Resource());
    List<Resource> b = new ArrayList<>();
    b.add(new Resource());
    for (Resource r : a) {
      r.close();
    }
    for (Resource r : b) {
      r.close();
      r.flush();
    }
  }
}
