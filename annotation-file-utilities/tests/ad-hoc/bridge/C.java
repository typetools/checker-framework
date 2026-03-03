@interface A {}

class C implements Comparable<C> {
  String value = "";

  @A public int compareTo(C other) {
    return value.compareTo(other.value);
  }
}
