@SuppressWarnings("all")
public class Issue2717 {

  interface Tree {}

  interface ExpressionTree extends Tree {}

  interface BinaryTree extends ExpressionTree {}

  interface Matcher<T extends Tree> {}

  public static void test(Matcher<? super ExpressionTree> m) {}

  public static void caller() {
    test(toType(BinaryTree.class, Issue2717.<BinaryTree>allOf()));
  }

  public static <T extends Tree> Matcher<T> allOf() {
    return null;
  }

  public static <S extends T, T extends Tree> Matcher<T> toType(
      Class<S> type, Matcher<? super S> matcher) {
    return null;
  }
}
