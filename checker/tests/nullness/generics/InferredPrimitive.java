/** Test case for Issue 143: https://github.com/typetools/checker-framework/issues/143 */
public class InferredPrimitive {
  public static void main(String[] args) {
    java.util.Set<Long> s = java.util.Collections.singleton(123L);
  }
}
