public abstract class RawInt2 {

  public void compare(MyVarInfo vi1, MyVarInfo vi2) {

    int name1in2 = 1;
    int name2in1 = 2;
    int cmp1 = (name1in2 == -1) ? 0 : vi1.varinfo_index - 1;
    // Removing this line eliminates the error, even though cmp2 is not used
    int cmp2 = false ? 0 : 15;
    sign(cmp1);
  }

  public void sign(int x) {}
}

final class MyVarInfo {
  public int varinfo_index = 22;
}
