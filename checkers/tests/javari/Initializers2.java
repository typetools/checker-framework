import checkers.javari.quals.*;
import java.awt.Point;

class Initializers2 {

    @ReadOnly Point a = new Point(0, 1);
    //:: (assignment.type.incompatible)
    Point b = a;       // cannot assign readonly to mutable
    //:: (primitive.ro)
    @ReadOnly int i;   // no readonly primitives

}
