public class FormatNullArray {

  public static void main(String[] args) {

    // All 4 lines are legal and produce the same output: "null null".
    System.out.printf("%s %s%n", null, null);
    System.out.printf("%d %d%n", null, null);
    System.out.printf("%s %s%n", (Object[]) null);
    System.out.printf("%d %d%n", (Object[]) null);
  }
}
