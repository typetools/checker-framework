
class Addressable<T extends Addressable> implements Comparable<T> {
    @dataflow.quals.Pure
    public int compareTo(T t) { return 0; }
}
