import checkers.units.quals.*;
import checkers.units.*;

public class Division {
  @m int m1, m2;
  int x = m1 / m2;

  //:: error: (assignment.type.incompatible)
  @m int bad = m1 / m2;

  // Division removes the unit.
  // As unqualified would be a supertype, we add another multiplication
  // to make sure the result of the division is unqualified.
  @s int div = (m1 / UnitsTools.m) * UnitsTools.s;
}
