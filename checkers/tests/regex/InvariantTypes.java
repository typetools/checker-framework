import checkers.regex.quals.*;

public class InvariantTypes {
  String[] sa = {"a"};
  @Regex String[] rsa = {"a"};
  //:: error: (type.incompatible) :: error: (assignment.type.incompatible)
  @Regex String[] rsaerr = {"(a"};
  String[] nrsa = {"(a"};
}
