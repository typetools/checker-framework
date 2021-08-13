public class Issue4877 {

  interface NInterface<T extends NInterface<T>> {}

  interface OInterface<T extends NInterface<T>> {}

  interface XInterface {}

  static class QClass<M extends NInterface<M> & XInterface> {}

  static final class PClass<T extends NInterface<T>> implements OInterface<T> {
    QClass<? extends T> f;
  }
}
