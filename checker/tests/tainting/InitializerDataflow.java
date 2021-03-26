import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.HasQualifierParameter;

public class InitializerDataflow {
  @HasQualifierParameter(Tainted.class)
  static class Buffer {}

  @PolyTainted Buffer id(@PolyTainted String s) {
    return null;
  }

  void methodBuffer(@Untainted String s) {
    Buffer b1 = id(s);

    String local = s;
    Buffer b2 = id(local);

    @Untainted String local2 = s;
    Buffer b3 = id(local2);
  }
}
