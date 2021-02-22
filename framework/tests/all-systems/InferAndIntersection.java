public class InferAndIntersection {

    <T> void toInfer(Iterable<T> t) {}

    <U extends Object & Iterable<Object>> void context(U u) {
        toInfer(u);
    }
}
