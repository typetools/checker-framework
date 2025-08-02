import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall({"flush", "close"})
class Resource implements AutoCloseable {
  @Override
  public void close() {}

  void flush() {}
}

// 2. check that Aggregator has MustCall method
class Aggregator implements Closeable {
  // 1. infer this field as @OwningCollection
  List<Resource> resList = new ArrayList<>();

  public Aggregator() {}

  public Aggregator(@OwningCollection List<Resource> list) {
    resList = list;
  }

  // 5. demand methods adding elements to have @CreatesMustCallFor("this")
  @CreatesMustCallFor("this")
  void add(@Owning Resource r) {
    resList.add(r);
  }

  // 3. check that mustcall method has a @CollectionFieldDestructor annotation
  // 4. check that field has @OCwO as postcondition
  @Override
  @CollectionFieldDestructor("resList")
  public void close() {
    for (Resource r : resList) {
      r.close();
      r.flush();
    }
  }

  // :: error: missing.creates.mustcall.for
  void addIllegal(Resource r) {
    resList.add(r);
  }

  @CollectionFieldDestructor("resList")
  // :: error: contracts.postcondition
  public void partialClose() {
    for (Resource r : resList) {
      r.flush();
    }
  }
}

// has no @MustCall method
class IllegalAggregator {
  // :: error: unfulfilled.collection.obligations
  List<Resource> resList = new ArrayList<>();

  @CollectionFieldDestructor("resList")
  public void close() {
    for (Resource r : resList) {
      r.close();
      r.flush();
    }
  }
}

public class OwningCollectionFieldTest {
  void addToAggregator(Resource @OwningCollection [] resources) {
    Aggregator agg = new Aggregator();
    for (Resource r : resources) {
      agg.add(r);
    }
    agg.close();

    // this is not necessary, but the checker would issue a false positive
    // without this closing loop.
    for (Resource r : resources) {
      r.close();
      r.flush();
    }
  }

  // Mainly test that accessing the owning resource collection field of another class is
  // forbidden.
  // since an obligaton for the field agg.resList is created, a @CreatesMustCallFor("agg")
  // annotation is expected, which is nonsensical (why the assignment of this field itself
  // is forbidden).
  // :: error: missing.creates.mustcall.for
  void accessOwningFieldAfterClosing(Resource @OwningCollection [] resources) {
    Aggregator agg = new Aggregator();
    for (Resource r : resources) {
      agg.add(r);
    }
    agg.close();
    // :: error: foreign.owningcollection.field.access
    agg.resList.add(new Resource());

    // this is not necessary, but the checker would issue a false positive
    // without this closing loop.
    for (Resource r : resources) {
      r.close();
      r.flush();
    }
  }

  void failToDestructAggregator(Resource @OwningCollection [] resources) {
    // :: error: required.method.not.called
    Aggregator agg = new Aggregator();
    for (Resource r : resources) {
      agg.add(r);
    }

    // this is not necessary, but the checker would issue a false positive
    // without this closing loop.
    for (Resource r : resources) {
      r.close();
      r.flush();
    }
  }

  void addAfterDestructing(Resource @OwningCollection [] resources) {
    // :: error: required.method.not.called
    Aggregator agg = new Aggregator();
    for (Resource r : resources) {
      agg.add(r);
    }
    agg.close();
    agg.add(new Resource());

    // this is not necessary, but the checker would issue a false positive
    // without this closing loop.
    for (Resource r : resources) {
      r.close();
      r.flush();
    }
  }
}
