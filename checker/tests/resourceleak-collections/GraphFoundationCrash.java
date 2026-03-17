import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall("close")
class CrashRepro {

  // TODO: Bug ArrayList l = new ArrayList<>() has JDK issues.
  private List<Resource> pool;

  CrashRepro(int maxSize) {
    this.pool = new ArrayList<>(maxSize); // should trigger the crash path
  }

  @CollectionFieldDestructor("this.pool")
  void close() {
    for (Resource r : pool) {
      r.flush();
      r.close();
    }
    pool.clear();
    pool = null;
  }

  public synchronized void prune() {
    //    if (pool == null) {
    //      return;
    //    }
    Iterator<Resource> itr = pool.iterator();
    while (itr.hasNext()) {
      Resource reader = itr.next();
    }
  }
}
