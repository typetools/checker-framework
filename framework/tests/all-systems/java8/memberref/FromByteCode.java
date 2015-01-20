
interface Function<T, R> {
    R apply(T t);
}
@SuppressWarnings("javari")
class FromByteCode {
    Function<String, String> f1 = String::toString;
}