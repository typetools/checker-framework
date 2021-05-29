import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class PostconditionBug {

  void a(@UnknownInitialization PostconditionBug this) {
    @NonNull String f = "abc";
    // :: error: (assignment)
    f = null;
  }
}
