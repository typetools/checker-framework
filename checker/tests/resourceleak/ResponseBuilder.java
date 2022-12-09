import org.checkerframework.checker.mustcall.qual.*;

public class ResponseBuilder {

  @Owning Builder b;

  @CreatesMustCallFor("this")
  void test(@Owning ResponseBody rb) {
    Builder b2 = new Builder();
    Builder b3 = b2;
    b2 = b2.body(rb);
    b = b3;
  }

  @SuppressWarnings("all")
  @MustCall("close")
  class ResponseBody {

  }

  @SuppressWarnings("all")
  class Builder {
    public @MustCallAlias Builder body(@MustCallAlias ResponseBody b) {
      return this;
    }
  }
}
