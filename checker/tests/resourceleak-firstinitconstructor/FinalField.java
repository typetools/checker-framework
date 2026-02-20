import java.io.FileInputStream;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall({"close"})
class FinalField {
  private int i;

  private final @Owning FileInputStream s;

  public FinalField() throws Exception {
    havoc();
    s = new FileInputStream("test.txt");
  }

  void havoc() {
    i++;
  }

  @EnsuresCalledMethods(value = "this.s", methods = "close")
  public void close() {
    try {
      s.close();
    } catch (Exception e) {
    }
  }
}
