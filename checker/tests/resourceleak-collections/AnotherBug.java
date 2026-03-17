import java.util.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall("close")
class OCInCons {
  List<Resource> list;

  public OCInCons(@OwningCollection List<Resource> list) {
    this.list = list;
  }

  @CollectionFieldDestructor("this.list")
  public void close() {
    for (Resource r : list) {
      r.close();
      r.flush();
    }
  }
}
