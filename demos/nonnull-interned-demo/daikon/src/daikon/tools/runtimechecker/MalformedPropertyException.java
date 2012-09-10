package daikon.tools.runtimechecker;

/**
 * Thrown when parsing the XML representation of a property, if the property is
 * not well-formed.
 */
public class MalformedPropertyException extends Exception {


    private static final long serialVersionUID = 1L;

    public MalformedPropertyException(String s) {
        super(s);
    }

}
