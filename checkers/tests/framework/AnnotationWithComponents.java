/** An Annotation can contain components besides the methods
 * declaring the annotation arguments, e.g. classes and fields.
 */
@interface Anno {
    class Inner {}
    int con = 5;

    int value();
}

class Use {
    @Anno(0) Object o;
}