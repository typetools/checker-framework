// Like SimpleLambdaParameter.java, but the invocation that uses the implicitly typed lambda
// parameter is in the body of a second, nested implicitly typed lambda.  Creating the constraints
// for `p.map("func")` requires the type of `p`, which is not known until inference has processed
// both lambdas.  This once caused a crash: "Expected the type of METHOD_INVOCATION tree in
// assignment context to be a functional interface."

import java.util.function.Function;

public abstract class NestedLambdaParameter {
  void method() {
    Function<Mapper, Function<Mapper, String>> mapper = identity(p -> q -> q.map(p.map("func")));
  }

  interface Mapper {
    <S> S map(S mapper);
  }

  abstract <Z> Z identity(Z p);
}
