import java.util.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class FinallyBug {
  void f(@OwningCollection List<Resource> col) {
    try {
      for (Resource r : col) {
        r.flush();
        r.close();
      }
    } finally {
      col.clear();
    }
  }

  void f2(@OwningCollection List<Resource> col) {
    try {
      for (Resource r : col) {
        r.flush();
        r.close();
      }
    } finally {

    }
    col.clear();
  }
}
