import java.util.*;

class FullyQualifiedAnnotation {

  // Making this Iterator<Object> resolves the problem
  void client(Iterator i) {

    @SuppressWarnings("nullness")
    /*@checkers.nullness.quals.NonNull*/ Object handle2 = i.next();
    handle2.toString();

  }

}
