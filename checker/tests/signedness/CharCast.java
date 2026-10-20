import org.checkerframework.checker.signedness.qual.SignedPositive;

public class CharCast {

  void m(@SignedPositive int i) {
    char c = (char) i;
  }

  void m1(short s) {
    int x = s;
    char c = (char) x;
  }

  void m2(int i) {
    int x = (short) i;
    char c = (char) x;
  }

  void m3() {
    int x = (short) 1;
    char c = (char) x;
  }

  void m4() {
    short x = 1;
    int y = x;
    char c = (char) y;
  }
}
