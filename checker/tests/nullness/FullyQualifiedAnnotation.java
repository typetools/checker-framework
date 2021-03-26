import java.util.Iterator;

public class FullyQualifiedAnnotation {

  void client1(Iterator i) {
    @SuppressWarnings("nullness")
    @org.checkerframework.checker.nullness.qual.NonNull Object handle2 = i.next();
    handle2.toString();
  }

  void client2(Iterator i) {
    @SuppressWarnings("nullness")
    @org.checkerframework.checker.nullness.qual.NonNull Object handle2 = i.next();
    handle2.toString();
  }

  void client3(Iterator<Object> i) {
    @SuppressWarnings("nullness")
    @org.checkerframework.checker.nullness.qual.NonNull Object handle2 = i.next();
    handle2.toString();
  }

  void client4(Iterator<Object> i) {
    @SuppressWarnings("nullness")
    @org.checkerframework.checker.nullness.qual.NonNull Object handle2 = i.next();
    handle2.toString();
  }
}
