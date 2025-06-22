import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall({"flush"})
class Resource implements AutoCloseable {
  @Override
  public void close() {}

  void flush() {}
}

class LoopBodyAnalysisTests {

  void fullSatisfyCollection(@OwningCollection Collection<Resource> resources) {
    for (Resource r : resources) {
      r.close();
      r.flush();
    }
    checkArgIsOCWO(resources);
  }

  void fullSatisfyArray(Resource @OwningCollection [] resources) {
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

  // here, the argument defaults to @NotOwningCollection.
  // the loop should not change that type
  void fullSatisfyArrayNotOwning(Resource[] resources) {
    for (Resource r : resources) {
      r.close();
      r.flush();
    }
    // :: error: argument
    checkArgIsOCWO(resources);
  }

  // :: error: unfulfilled.collection.obligations
  void partialSatisfyArrayShouldError(Resource @OwningCollection [] resources) {
    for (Resource r : resources) {
      r.close();
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
    // :: error: unfulfilled.collection.obligations
    List<Resource> l = new ArrayList<>();
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

  void tryCatchShouldWork(Resource @OwningCollection [] resources) {
    for (Resource r : resources) {
      try {
        r.close();
        r.flush();
      } catch (Exception e) {
      }
    }
  }

  void methodCallInsideLoop(Resource @OwningCollection [] resources) {
    for (Resource r : resources) {
      doCloseFlush(r);
    }
  }

  // :: error: unfulfilled.collection.obligations
  void earlyBreak(Resource @OwningCollection [] resources) {
    for (Resource r : resources) {
      r.close();
      r.flush();
      break; // not all elements visited
    }
  }

  void tryWithResources(Resource @OwningCollection [] resources) {
    for (Resource r : resources) {
      try (Resource auto = r) {
        auto.flush();
      }
    }
  }

  void nullableElementWithCheck(Resource @OwningCollection [] resources) {
    for (Resource r : resources) {
      if (r != null) {
        r.close();
        r.flush();
      }
    }
  }

  void nullableElementHelper(Resource @OwningCollection [] resources) {
    for (Resource r : resources) {
      if (r != null) {
        doCloseFlush(r);
      }
    }
  }

  @EnsuresCalledMethods(
      value = "#1",
      methods = {"close", "flush"})
  void doCloseFlush(Resource r) {
    r.close();
    r.flush();
  }

  void indexForLoop(Resource @OwningCollection [] resources) {
    for (int i = 0; i < resources.length; i++) {
      resources[i].close();
      resources[i].flush();
    }
  }

  // :: error: unfulfilled.collection.obligations
  void indexForLoopPartial(Resource @OwningCollection [] resources) {
    for (int i = 0; i < resources.length; i++) {
      resources[i].close();
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

  void checkArgIsOCWO(@OwningCollectionWithoutObligation Iterable<Resource> arg) {}

  void checkArgIsOCWO(Resource @OwningCollectionWithoutObligation [] arg) {}
}
