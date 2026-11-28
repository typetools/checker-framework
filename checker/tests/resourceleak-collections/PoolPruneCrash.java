import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

/*
 * Crash reproducer for pruning a pooled owning collection after it was initialized with an
 * ArrayList(int) constructor.
 */
@InheritableMustCall("close")
class PoolPruneCrash {

  private List<Resource> pool;

  PoolPruneCrash(int maxSize) {
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
    if (pool == null) {
      return;
    }
    Iterator<Resource> itr = pool.iterator();
    while (itr.hasNext()) {
      Resource reader = itr.next();
    }
  }
}
