public class Bug109_A {
  int one = "1".length();

  // fix 1: public final int one; { one = "1".length(); }
  // fix 2: public final int one = 0 + "1".length();

  int nl = 5;
  int two = nl;
}
