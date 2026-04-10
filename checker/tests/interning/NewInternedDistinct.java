import org.checkerframework.checker.interning.qual.InternedDistinct;

public class NewInternedDistinct {

  public void foo() {
    @InternedDistinct Object o = new Object();
  }
}
