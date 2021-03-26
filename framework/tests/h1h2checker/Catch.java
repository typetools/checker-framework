import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

public class Catch {
  void defaultUnionType() throws Throwable {
    try {
      throw new Throwable();
    } catch (IndexOutOfBoundsException | NullPointerException ex) {

    }
  }

  void defaultDeclaredType() throws Throwable {
    try {
      throw new Throwable();
    } catch (RuntimeException ex) {

    }
  }

  void explictlyTopUnionType() throws Throwable {
    try {
      throw new Throwable();
    } catch (@H1Top @H2Top IndexOutOfBoundsException | @H1Top @H2Top NullPointerException ex) {

    }
  }

  void explictlyNotTopUnionType() throws Throwable {
    try {
      throw new Throwable();
      // :: error: (exception.parameter.invalid)
    } catch (@H1S1 @H2Top IndexOutOfBoundsException | @H1S1 @H2Top NullPointerException ex) {

    }
  }

  void explictlyTopDeclaredType() throws Throwable {
    try {
      throw new Throwable();
    } catch (@H1Top @H2Top NullPointerException ex) {

    }
  }

  void explictlyNotTopDeclaredType() throws Throwable {
    try {
      throw new Throwable();
      // :: error: (exception.parameter.invalid)
    } catch (@H1S1 @H2Top RuntimeException ex) {

    }
  }
}
