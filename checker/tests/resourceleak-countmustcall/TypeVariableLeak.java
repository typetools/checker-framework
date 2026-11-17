// Test input for ResourceLeakCountMustCallTest.

import java.io.Closeable;
import org.checkerframework.checker.mustcall.qual.Owning;

class TypeVariableLeak {
  // The Resource Leak Checker reports required.method.not.called here.
  <T extends Closeable> void leakParameter(@Owning T t) {}
}
