public class IterableGenerics {
  interface Data extends Iterable<String> {}

  <T extends Data> void typeParam(T t) {
    for (String s : t)
      ;
  }

  void wildcard(Iterable<? extends Data> t) {
    for (Object a : t.iterator().next())
      ;
  }
}
