public class AdditionWithChar {
  int i2;

  void additionWithChar(int i1, char c) {
    // :: error: (assignment) :: error: (operation.mixed.unsignedrhs)
    i2 = i1 + c;
  }
}
