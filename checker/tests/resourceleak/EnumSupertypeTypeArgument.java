// The supertype of an enum class E is `Enum<E>`, whose type argument has the same qualifier as the
// enum class itself.  Because that type argument is not invariant, type inference must not compare
// its qualifier to the qualifier of another type argument.  Here, the target type is
// @MustCallUnknown MyEnum, but the arguments make the type argument @MustCall MyEnum.
public class EnumSupertypeTypeArgument {
  enum MyEnum {
    ONE,
    TWO
  }

  static <T extends Enum<T>> T get(Class<T> expectedType, T defaultValue) {
    return defaultValue;
  }

  void use() {
    MyEnum x = get(MyEnum.class, MyEnum.ONE);
  }
}
