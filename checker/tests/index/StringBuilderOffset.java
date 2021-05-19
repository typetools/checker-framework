public class StringBuilderOffset {
  public static void OffsetStringBuilder() {
    StringBuilder stringBuilder = new StringBuilder();
    char[] chars = new char[10];

    // :: error: (argument)
    stringBuilder.append(chars, 5, 7);

    stringBuilder.append(chars, 5, 4);
  }
}
