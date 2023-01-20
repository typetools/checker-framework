class SelfRef<K extends Comparable<K>, V> {
  public SelfRef<K, V> parent, left, right;

  public SelfRef(SelfRef<K, V> parent) {
    this.parent = parent;
  }
}
