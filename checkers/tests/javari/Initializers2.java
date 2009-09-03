import checkers.javari.quals.*;
import java.awt.Point;

class Initializers2 {

    @ReadOnly Point a = new Point(0, 1);
    Point b = a;       // cannot assign readonly to mutable
    @ReadOnly int i;   // no readonly primitives

}
