import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

public class ForEach {

  Object arrayAccess1(Object[] constants) {
    Object constant = constants[0];
    return constant;
  }

  @H1S2 Object arrayAccessBad1(@H1S1 Object[] constants) {
    Object constant = constants[0];
    // :: error: (return)
    return constant;
  }

  // Return type defaults to H1Top
  @H2S1 Object arrayAccessBad2(@H1S1 @H2S2 Object[] constants) {
    Object constant = constants[0];
    // :: error: (return)
    return constant;
  }

  Object iterateFor(Object[] constants) {
    for (int i = 0; i < constants.length; ++i) {
      Object constant = constants[i];
      return constant;
    }
    return null;
  }

  Object iterateForEach(Object[] constants) {
    for (Object constant : constants) {
      return constant;
    }
    return null;
  }

  @H2S2 Object iterateForEachBad(@H2S1 Object[] constants) {
    for (Object constant : constants) {
      // :: error: (return)
      return constant;
    }
    return null;
  }

  // Now with a method type variable

  <T extends Object> T garrayAccess1(T[] constants) {
    return constants[0];
  }

  <T extends Object> T garrayAccess1(T p) {
    T constant = p;
    return constant;
  }

  <T extends Object> @H1S2 T garrayAccessBad1(@H1S1 T[] constants) {
    T constant = constants[0];
    // :: error: (return)
    return constant;
  }

  // Return type defaults to H1Top
  <T extends Object> @H2S1 T garrayAccessBad2(@H1S1 @H2S2 T[] constants) {
    T constant = constants[0];
    // :: error: (return)
    return constant;
  }

  <T extends Object> T giterateFor(T[] constants) {
    for (int i = 0; i < constants.length; ++i) {
      T constant = constants[i];
      return constant;
    }
    return null;
  }

  <T extends Object> T giterateForEach(T[] constants) {
    for (T constant : constants) {
      return constant;
    }
    return null;
  }

  <T extends Object> @H2S2 T giterateForEachBad(@H2S1 T[] constants) {
    for (T constant : constants) {
      // :: error: (return)
      return constant;
    }
    return null;
  }
}
