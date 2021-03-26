import org.checkerframework.checker.interning.qual.Interned;

public class CompileTimeConstants2 {
  @Interned String s1 = "" + ("" + 1);

  @Interned String s2 = (("" + ("" + 1)));

  @Interned String s3 = ("" + (("")) + 1);

  @Interned String s4 = "" + Math.PI;

  // To make sure that we would get an error if the RHS is not interned
  // :: error: (assignment.type.incompatible)
  @Interned String err = "" + new Object();
}
