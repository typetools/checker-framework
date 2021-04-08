import org.checkerframework.checker.lock.qual.GuardedBy;

public class Update {

  void test() {
    Object o1 = new Object();
    @GuardedBy({}) Object o2 = o1;
    synchronized (o1) {
    }
    // o1 used to loss it refinement because of a bug.
    @GuardedBy({}) Object o3 = o1;
  }
}
