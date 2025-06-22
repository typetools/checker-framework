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

  void fullSatisfyArray(Resource @OwningCollection [] resources) {
    for (Resource r : resources) {
      r.close();
      r.flush();
    }
  }

  void fullSatisfyCollection(@OwningCollection Collection<Resource> resources) {
    for (Resource r : resources) {
      r.close();
      r.flush();
    }
  }

  // :: error: unfulfilled.collection.obligations
  void partialSatisfyArrayShouldError(Resource @OwningCollection [] resources) {
    for (Resource r : resources) {
      r.close();
    }
  }

  // :: error: unfulfilled.collection.obligations
  void partialSatisfyCollectionShouldError(@OwningCollection Collection<Resource> resources) {
    for (Resource r : resources) {
      r.close();
    }
  }

  void multipleMustCallPartial() {
    // :: error: unfulfilled.collection.obligations
    List<Resource> l = new ArrayList<>();
    l.add(new Resource());
    l.add(new Resource());
    for (Resource r : l) {
      r.close();
    }
  }

  void multipleMustCallFull() {
    List<Resource> l = new ArrayList<>();
    l.add(new Resource());
    l.add(new Resource());
    for (Resource r : l) {
      r.close();
      r.flush();
    }
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
}
