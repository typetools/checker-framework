package daikon.chicory;

/*
 * Created on Feb 2, 2005
 *
 */

/**
 * A NonsensicalObject is used during data trace output for variables whose
 * value is "nonsensical" to print.  For instance, say class A has a field name.
 * If variable x is of type A is null, then we print "null" for x's value.  However,
 * we print "nonsensical" for x.name's value.
 */
public class NonsensicalObject
{
    private static NonsensicalObject instance = new NonsensicalObject();

    private NonsensicalObject() {}

    public static NonsensicalObject getInstance() {return instance;}

}
