package annotations.tests.classfile.cases;

public class TestClassNonEmpty {
  public int i;
  private String a;

  private TestClassNonEmpty() {
    i = 0;
  }

  protected TestClassNonEmpty(String s) {
    a = s;
  }

  public int i() {
    return i;
  }

  public String a() {
    String s = new String(a);
    s = s + s;
    return s;
  }

}
