// Assigning to a local variable is not a side effect that is visible outside the method, so it
// need not be listed in `@SideEffectsOnly`.

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class LocalVariableSeonly {

  @SideEffectsOnly("#1")
  void assignPrimitiveLocal(List<String> lst) {
    int i = 0;
    i = i + 1;
    i += 2;
    i++;
    ++i;
  }

  @SideEffectsOnly("#1")
  void assignReferenceLocal(List<String> lst) {
    List<String> other = new ArrayList<>();
    other = new ArrayList<>();
  }

  // `this` is unrelated to the locals, so nothing here is covered by the annotation.
  @SideEffectsOnly("this")
  void assignLocalOnly() {
    int i = 0;
    i = i + 1;
    List<String> other = new ArrayList<>();
    other = new ArrayList<>();
  }
}
