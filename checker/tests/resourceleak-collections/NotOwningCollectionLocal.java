import java.io.InputStream;
import java.util.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

/*
 * Tests that a local explicitly typed as @NotOwningCollection retains that type through
 * mutation and iterator creation.
 */
class NotOwningCollectionLocal {
  @NotOwning
  InputStream newNonOwningStream() {
    throw new Error();
  }

  @NotOwningCollection
  Iterator<InputStream> iteratorFromNotOwningLocal() {
    @NotOwningCollection List<InputStream> list = new ArrayList<>();
    list.add(newNonOwningStream());
    return list.iterator();
  }
}
