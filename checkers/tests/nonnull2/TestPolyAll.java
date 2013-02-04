import checkers.quals.PolyAll;
import checkers.nullness.quals.*;

// Same test as TestPolyNull, just using PolyAll as qualifier.
// Behavior must be the same.
class TestPolyNull {
   void identity(@PolyAll String str) {  }
}
