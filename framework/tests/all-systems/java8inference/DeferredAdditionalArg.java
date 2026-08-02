// Exercises the deferred additional-argument constraint path of JLS 18.5.2.2, which is
// handled by the loop in InvocationTypeInference.getB4.
//
// Each lambda below has an implicitly typed parameter whose body contains a generic method
// invocation or diamond constructor invocation that mentions that parameter.  The additional
// argument constraint for the nested invocation cannot be created until the lambda parameter
// has a type, so InvocationTypeInference.createAdditionalArgConstraintsNoLambda defers it by
// adding an unreduced AdditionalArgument constraint to C.  That constraint is reduced later,
// inside getB4, where reducing it creates a new Theta -- and therefore new inference
// variables -- that are mentioned by the constraints added back to C.
//
// Before this test, SimpleLambdaParameter.java was the only test that reached the deferred
// path, and its nested invocation is not itself generic, so it introduces no new inference
// variables.

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
