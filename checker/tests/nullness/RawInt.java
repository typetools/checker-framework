public class RawInt {

  public void compare(int name1in2, int name2in1, VarInfo vi) {
    int cmp1 = (name1in2 == -1) ? 0 : vi.varinfo_index - name1in2;
    MathMDE.sign(cmp1);
  }
}

class VarInfo {
  int varinfo_index;
}

class MathMDE {
  public static void sign(int a) {}
}
