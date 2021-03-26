public class WildcardIn {

  void foo(GenericObject<?> gen) {
    Integer i = (Integer) gen.get();
  }
}

interface GenericObject<T> {
  public abstract T get();
}
