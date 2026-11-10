// Passing a resource collection to a method whose parameter has an unbounded wildcard or type
// variable as its type argument, such as `List<?>` or `<T> List<T>`.  Such a method cannot add a
// resource to the collection, so passing a resource collection to it is safe.

import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;

class UnboundedTypeArgumentParameter {

  static int countWildcard(List<?> list) {
    return list.size();
  }

  static <T> int countTypeVariable(List<T> list) {
    return list.size();
  }

  void passToWildcard(@OwningCollection List<Resource> resources) {
    // TODO: This is a false positive.  The Must Call Checker defaults the wildcard in `List<?>`
    // to @MustCall({}), so `List<Resource>` is not a subtype of the parameter type.
    // :: error: argument
    countWildcard(resources);
    for (Resource r : resources) {
      r.close();
      r.flush();
    }
  }

  void passToTypeVariable(@OwningCollection List<Resource> resources) {
    // TODO: These are false positives.  The Must Call Checker defaults the type variable in
    // `List<T>` to @MustCall({}), and the Collection Ownership Checker defaults the parameter to
    // @OwningCollectionBottom.
    // :: error: type.arguments.not.inferred
    // :: error: argument
    countTypeVariable(resources);
    for (Resource r : resources) {
      r.close();
      r.flush();
    }
  }
}
