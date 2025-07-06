import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class LoopBodyAnalysisTests {

  void fullSatisfyCollection(@OwningCollection Collection<Resource> resources) {
    for (Resource r : resources) {
      r.close();
      r.flush();
    }
    checkArgIsOCWO(resources);
  }

  // here, the argument defaults to @NotOwningCollection.
  // the loop should not change that type
  void fullSatisfyCollectionNotOwning(Collection<Resource> resources) {
    for (Resource r : resources) {
      r.close();
      r.flush();
    }
    // :: error: argument
    checkArgIsOCWO(resources);
  }

  // :: error: unfulfilled.collection.obligations
  void partialSatisfyCollectionShouldError(@OwningCollection Collection<Resource> resources) {
    for (Resource r : resources) {
      r.close();
    }
    // :: error: argument
    checkArgIsOCWO(resources);
  }

  void multipleMustCallPartial() {
    List<Resource> l = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    l.add(new Resource());
    for (Resource r : l) {
      r.close();
    }
    // :: error: argument
    checkArgIsOCWO(l);
  }

  void multipleMustCallFull() {
    List<Resource> l = new ArrayList<>();
    l.add(new Resource());
    for (Resource r : l) {
      r.close();
      r.flush();
    }
    checkArgIsOCWO(l);
  }

  void tryCatchShouldWork(@OwningCollection List<Resource> resources) {
    for (Resource r : resources) {
      try {
        r.close();
        r.flush();
      } catch (Exception e) {
      }
    }
  }

  void methodCallInsideLoop(@OwningCollection List<Resource> resources) {
    for (Resource r : resources) {
      doCloseFlush(r);
    }
  }

  // :: error: unfulfilled.collection.obligations
  void earlyBreak(@OwningCollection List<Resource> resources) {
    for (Resource r : resources) {
      r.close();
      r.flush();
      break; // not all elements visited
    }
  }

  // TODO SCK: uncomment these tests
  // void tryWithResources(@OwningCollection List<Resource> resources) {
  //   for (Resource r : resources) {
  //     try (Resource auto = r) {
  //       auto.flush();
  //     }
  //   }
  // }

  // void nullableElementWithCheck(@OwningCollection List<Resource> resources) {
  //   for (Resource r : resources) {
  //     if (r != null) {
  //       r.close();
  //       r.flush();
  //     }
  //   }
  // }

  // void nullableElementHelper(@OwningCollection List<Resource> resources) {
  //   for (Resource r : resources) {
  //     if (r != null) {
  //       doCloseFlush(r);
  //     }
  //   }
  // }

  @EnsuresCalledMethods(
      value = "#1",
      methods = {"close", "flush"})
  void doCloseFlush(Resource r) {
    r.close();
    r.flush();
  }

  void indexForLoop(@OwningCollection List<Resource> resources) {
    for (int i = 0; i < resources.size(); i++) {
      resources.get(i).close();
      resources.get(i).flush();
    }
  }

  // :: error: unfulfilled.collection.obligations
  void indexForLoopPartial(@OwningCollection List<Resource> resources) {
    for (int i = 0; i < resources.size(); i++) {
      resources.get(i).close();
      // missing flush
    }
  }

  void indexForLoopList(@OwningCollection List<Resource> resources) {
    for (int i = 0; i < resources.size(); i++) {
      Resource r = resources.get(i);
      r.close();
      r.flush();
    }
  }

  // :: error: illegal.type.annotation
  void checkArgIsOCWO(@OwningCollectionWithoutObligation Iterable<Resource> arg) {}
}
