/* @skip-test */
public final class ArraysMDETest {

  public static @PolyInterned Object[] subarray(@PolyInterned Object[] a, int startindex, int length) {
    @PolyInterned Object[] result = new @PolyInterned Object[length];
    System.arraycopy(a, startindex, result, 0, length);
    return result;
  }


}
