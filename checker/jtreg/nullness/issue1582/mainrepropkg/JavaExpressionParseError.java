package mainrepropkg;

import foo.Foo;

public class JavaExpressionParseError {

  public void printAThing(Foo foo) {
    if (foo.hasTheObject()) {
      System.out.println("Print false: " + foo.getTheObject().equals(new Object()));
    }
  }
}
