package typearginfer;

import java.util.Set;
import java.util.concurrent.Callable;

public abstract class Issue6753 {

  abstract <E> E call(Callable<E> task);

  abstract <T> Set<T> list();

  void method(int length) {
    Set<String> resources =
        call(
            () -> {
              if (length > 1) {
                return list();
              } else {
                return list();
              }
            });
  }
}
