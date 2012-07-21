import checkers.regex.quals.*;

public class InvariantTypes {
  String[] sa = {"a"};
  String[] sa2 = {"a", "b"};
  public String[] sa3 = {"a", "b"};
  public static  String[] sa4 = {"a", "b"};
  public final String[] sa5 = {"a", "b"};
  public static final String[] sa6 = {"a", "b"};
  final String[] sa7 = {"a", "b"};

  @Regex String[] rsa = {"a"};
  //:: error: (type.incompatible) :: error: (assignment.type.incompatible)
  @Regex String[] rsaerr = {"(a"};
  String[] nrsa = {"(a"};

  void unqm(String[] sa) {}
  void rem(@Regex String[] rsa) {}

  void recalls() {
    unqm(new String[] {"a"});
    //TODOINVARR:: error: (argument.type.incompatible)
    unqm(new @Regex String[] {"a"});
    // TODO: would we want the following to work?
    //:: error: (argument.type.incompatible)
    rem(new String[] {"a"});
    rem(new @Regex String[] {"a"});
  }

  void unqcalls() {
    unqm(new String[] {"a("});
    //TODOINVARR:: error: (argument.type.incompatible)
    //:: error: (type.incompatible)
    unqm(new @Regex String[] {"a("});
    //:: error: (argument.type.incompatible)
    rem(new String[] {"a("});
    //:: error: (type.incompatible)
    rem(new @Regex String[] {"a("});
  }

  // method argument context

    String[] retunqm(String[] sa) { return sa;}
    @Regex String[] retrem(@Regex String[] rsa) { return rsa; }
    @Regex String[] mixedm( String[] rsa) { return null; }

    void retunqcalls() {
        @Regex String[] re = mixedm(new String[] {"a("});
        //TODOINVARR:: error: (argument.type.incompatible)
        String [] u = retunqm(new String[] {"a"});
        //TODOINVARR:: error: (argument.type.incompatible)
        re = mixedm(new String[2]);
    }
}
