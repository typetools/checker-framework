// test case for https://github.com/typetools/checker-framework/issues/6990

import java.io.Closeable;
import org.checkerframework.checker.mustcall.qual.MustCallUnknown;
import org.checkerframework.checker.mustcall.qual.Owning;

public class DropOwning {

  public void f(@Owning Closeable resource) {
    drop(resource);
  }

  // :: error: required.method.not.known
  private void drop(@Owning @MustCallUnknown Object resourceCF6990) {}
}
