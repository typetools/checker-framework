/**
 * Test case for Issue 143:
 * http://code.google.com/p/checker-framework/issues/detail?id=143
 */
class InferredPrimitive {
  public static void main( String[] args ) {
    java.util.Set<Long> s = java.util.Collections.singleton( 123L );
  }
}
