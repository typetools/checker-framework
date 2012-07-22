package daikon.simplify;

/**
 * Superclass of all checked exceptions in this package.
 **/
public class SimplifyException
  extends Exception
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  public SimplifyException() { super(); }
  public SimplifyException(String s) { super(s); }
}
