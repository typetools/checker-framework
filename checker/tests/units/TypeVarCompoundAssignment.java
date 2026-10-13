import org.checkerframework.checker.units.qual.m;

public class TypeVarCompoundAssignment {
  <T extends Integer> void scalar(T factor, @m int distance) {
    distance *= factor;
    distance /= factor;
  }

  <T extends @m Integer> void units(T factor, @m int distance) {
    // :: error: [compound.assignment]
    distance *= factor;
    // :: error: [compound.assignment]
    distance /= factor;
  }
}
