public class CharCastedToInt {
  int charCastToInt(char c) {
    // :: error: (argument)
    intParameter((int) c);
    // :: error: (return)
    return (int) c;
  }

  void intParameter(int x) {}
}
