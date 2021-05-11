import org.checkerframework.checker.units.qual.kg;

public class UnqualTest {
  // :: error: (assignment)
  @kg int kg = 5;
  int nonkg = kg;
  // :: error: (assignment)
  @kg int alsokg = nonkg;
}
