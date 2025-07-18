import java.util.function.Function;

public abstract class SimpleLambdaParameter {
  void method() {
    Function<Mapper, String> mapper = identity(p -> p.map("func"));
  }

  interface Mapper {
    <S> S map(S mapper);
  }

  abstract <Z> Z identity(Z p);
}
