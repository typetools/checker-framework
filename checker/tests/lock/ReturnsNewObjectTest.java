import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.NewObject;

public class ReturnsNewObjectTest {
  @NewObject Object factoryMethod() {
    return new Object();
  }

  void m() {
    @GuardedBy("this") Object x = factoryMethod();
  }
}
