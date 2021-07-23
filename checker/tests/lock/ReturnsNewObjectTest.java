import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.NewObject;

public class ReturnsNewObjectTest {
    @NewObject Object factoryMethod() {
        return new Object();
    }

    void m() {
        @GuardedBy("this") Object x = factoryMethod();
    }

    void m2() {
        String @GuardedBy("this") [] a2 = new String[4];
        String @GuardedBy("this") [] a3 = new String[] {"a", "b", "c"};
    }
}
