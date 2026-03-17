import java.io.InputStream;
import java.util.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class NotOwningLocal {
  @NotOwning
  InputStream get() {
    throw new Error();
  }

  @NotOwningCollection
  Iterator<InputStream> test() {
    @NotOwningCollection List<InputStream> list = new ArrayList<>();
    list.add(get());
    return list.iterator();
  }
}
