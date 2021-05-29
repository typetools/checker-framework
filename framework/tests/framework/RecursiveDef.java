public class RecursiveDef<T extends RecursiveDef> implements Comparable<T> {
  @org.checkerframework.dataflow.qual.Pure
  public int compareTo(T t) {
    return 0;
  }
}
