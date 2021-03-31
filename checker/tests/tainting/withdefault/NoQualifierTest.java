package withdefault;

import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.framework.qual.NoQualifierParameter;

@NoQualifierParameter(Tainted.class)
public class NoQualifierTest {
  // :: error: (invalid.polymorphic.qualifier.use)
  @PolyTainted int field;
}
