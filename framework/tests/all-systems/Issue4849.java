// The error is reproducable only if the classes are not inner classes.
final class Issue4849F<T> {}

abstract class Issue4849C<F1> extends Issue4849G<Issue4849C<F1>> {}

abstract class Issue4849G<M extends Issue4849G<M>> {
  abstract M e();
}

@SuppressWarnings("all") // Just check for crashes.
abstract class Issue4849 {
  enum MyEnum {}

  abstract <F1> C<F1> n(Issue4849F<F1> field1);

  abstract <E extends Enum<E>> Issue4849F<E> of(Class<E> e);

  void method() {
    C<MyEnum> c = this.n(this.of(MyEnum.class)).e();
  }
}
