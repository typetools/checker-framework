// Test input for ResourceLeakCountMustCallTest.

import java.io.Closeable;
import java.io.IOException;
import org.checkerframework.checker.mustcall.qual.Owning;

class TypeVariable {
  <T extends Closeable> void closeParameter(@Owning T t) throws IOException {
    t.close();
  }
}
