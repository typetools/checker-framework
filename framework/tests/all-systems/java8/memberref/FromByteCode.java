interface FunctionFromByteCode<T, R> {
  R apply(T t);
}

public class FromByteCode {
  FunctionFromByteCode<String, String> f1 = String::toString;
}
