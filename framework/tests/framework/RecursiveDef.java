class Addressable<T extends Addressable> implements Comparable<T> {
    @org.checkerframework.dataflow.qual.Pure
    public int compareTo(T t) {
        return 0;
    }
}
