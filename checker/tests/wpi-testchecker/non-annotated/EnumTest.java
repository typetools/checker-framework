public class EnumTest {
  public enum MyEnum {
    ONE("ONE"),
    TWO("TWO"),
    THREE("THREE");

    private final String value;

    private MyEnum(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }

    public static MyEnum fromValue(String value) throws IllegalArgumentException {
      for (MyEnum method : MyEnum.values()) {
        String methodString = method.toString();
        if (methodString != null && methodString.equals(value)) {
          return method;
        }
      }

      throw new IllegalArgumentException("Cannot create enum from: " + value);
    }
  }
}
