import java.io.IOException;
import java.util.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class CollectionFieldDestructorExceptionPathTest {

  @InheritableMustCall("close")
  static class Resource2 {
    void close() throws IOException {}
  }

  void client3() {
    Resource2 r = new Resource2();
    try {
      r.close();
    } catch (IOException e) {
    }
    System.out.println();
  }
}
