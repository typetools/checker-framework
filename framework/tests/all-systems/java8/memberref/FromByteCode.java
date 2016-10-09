
interface Function<T, R> {
    R apply(T t);
}
class FromByteCode {
    Function<String, String> f1 = String::toString;
}
