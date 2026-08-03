// Exercises the deferred additional-argument constraint path of JLS 18.5.2.2, which is
// handled by the loop in InvocationTypeInference.getB4.

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings("unused") // the locals exist only to supply a target type
public abstract class DeferredAdditionalArg {

  void nestedGenericCall() {
    Function<Mapper, List<String>> f = id(p -> wrap(p.map("s")));
  }

  void twoNestedGenericCalls() {
    Function<Mapper, String> f = id(p -> choose(p.map("a"), p.map("b")));
  }

  void nestedDiamondConstructor() {
    Function<Mapper, ArrayList<String>> f = id(p -> new ArrayList<>(p.map(List.of("a"))));
  }

  interface Mapper {
    <S> S map(S s);
  }

  abstract <Z> Z id(Z p);

  abstract <T> List<T> wrap(T t);

  abstract <T> T choose(T a, T b);
}
