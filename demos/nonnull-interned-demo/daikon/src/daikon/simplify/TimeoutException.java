package daikon.simplify;

/**
 * Indicates a request timed out.
 **/
public class TimeoutException
  extends SimplifyException
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  public TimeoutException() { super(); }
  public TimeoutException(String s) { super(s); }
}
