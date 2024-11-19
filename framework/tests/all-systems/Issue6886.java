package typearginfer;

import java.util.List;
import java.util.function.Supplier;

abstract class Issue6886 {

  abstract <T> List<T> run(Supplier<? extends List<? extends T>> param);

  List<Void> call(List<Void> data) {
    return run(() -> data);
  }
}
