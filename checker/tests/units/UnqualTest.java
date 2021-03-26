import org.checkerframework.checker.units.qual.kg;

public class UnqualTest {
  // :: error: (assignment.type.incompatible)
  @kg int kg = 5;
  int nonkg = kg;
  // :: error: (assignment.type.incompatible)
  @kg int alsokg = nonkg;
}
