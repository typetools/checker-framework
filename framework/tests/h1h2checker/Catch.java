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

  void explicitlyTopUnionType() throws Throwable {
    try {
      throw new Throwable();
    } catch (@H1Top @H2Top IndexOutOfBoundsException | @H1Top @H2Top NullPointerException ex) {

    }
  }

  void explicitlyNotTopUnionType() throws Throwable {
    try {
      throw new Throwable();
      // :: error: [exception.parameter]
    } catch (@H1S1 @H2Top IndexOutOfBoundsException | @H1S1 @H2Top NullPointerException ex) {

    }
  }

  void explicitlyTopDeclaredType() throws Throwable {
    try {
      throw new Throwable();
    } catch (@H1Top @H2Top NullPointerException ex) {

    }
  }

  void explicitlyNotTopDeclaredType() throws Throwable {
    try {
      throw new Throwable();
      // :: error: [exception.parameter]
    } catch (@H1S1 @H2Top RuntimeException ex) {

    }
  }
}
