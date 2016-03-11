//@skip-tests
import org.checkerframework.checker.lock.qual.GuardedBy;

public class Simple {
   Object lock1, lock2;

   void testMethodCall(@GuardedBy("lock1") Simple this) {
       synchronized(lock1) {}
       synchronized(this.lock1) {}
       synchronized(lock2) {}
       synchronized(this.lock2) {}
       
       @GuardedBy("myClass.field") MyClass myClass = new MyClass();
       synchronized(myClass.field){}
       synchronized(myClass){}
   }

class MyClass{
  Object field;
}
}
