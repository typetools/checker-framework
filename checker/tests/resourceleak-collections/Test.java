import java.io.IOException;
import java.util.*;
import java.util.Iterator;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class CollectionFieldDestructorExceptionPathTest {

  @InheritableMustCall("close")
  static class Resource2 {
    void close() throws IOException {}
  }

  @InheritableMustCall("shutdownConnections")
  static class Worker {

    final @OwningCollection List<Resource2> tcpConnections = new ArrayList<>();

    @CreatesMustCallFor("this")
    void add(@Owning Resource2 resource) {
      tcpConnections.add(resource);
    }

    @CollectionFieldDestructor("this.tcpConnections")
    // ::error: contracts.postcondition
    void shutdownConnections() throws IOException {
      Iterator<Resource2> it = tcpConnections.iterator();
      while (it.hasNext()) {
        Resource2 r = it.next();
        r.close();
      }
    }
  }

  @InheritableMustCall("shutdownConnections")
  static class Worker2 {

    final @OwningCollection List<Resource2> tcpConnections = new ArrayList<>();

    @CreatesMustCallFor("this")
    void add(@Owning Resource2 resource) {
      tcpConnections.add(resource);
    }

    @CollectionFieldDestructor("this.tcpConnections")
    void shutdownConnections() throws IOException {
      Iterator<Resource2> it = tcpConnections.iterator();
      while (it.hasNext()) {
        Resource2 r = it.next();
        try {
          r.close();
        } catch (IOException e) {
        }
      }
    }
  }

  // :: error: illegal.type.annotation
  static void checkArgIsOCWO(@OwningCollectionWithoutObligation Iterable<Resource2> arg) {}

  void client() throws Exception {
    Worker w = new Worker();
    w.add(new Resource2());
    w.shutdownConnections();
  }

  void client2() {
    Worker w = new Worker();
    w.add(new Resource2());
    try {
      w.shutdownConnections();
    } catch (IOException e) {
    }
  }

  void client3() {
    Resource2 r = new Resource2();
    try {
      r.close();
    } catch (IOException e) {
    }
  }
}
