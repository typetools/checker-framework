public class First {
    public interface Supplier<T> {}

    public interface Callable<T> {}

    public static <T> void method(Supplier<T> supplier, Callable<? super T> callable) {}
}
