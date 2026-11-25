// An array initializer that is a constant is narrowed to the component type's primitive type
// before it is boxed (JLS 5.2).
public class ArrayInitializerNarrowingBoxing {
  void narrowAndBox() {
    Byte[] bytes = new Byte[] {1, 2};
    Short[] shorts = new Short[] {1, 'a'};
    Character[] chars = new Character[] {65, (short) 66};
  }

  void unbox(Object o) {
    int i = (int) o;
    long l = (long) o;
  }
}
