public class CompoundAssignmentCheck {
  void test() {
    int a = 9;
    a += 5;
    a -= 2;
    int[] arr5 = new int[a]; // LBC shouldn't warn here.
  }
}
