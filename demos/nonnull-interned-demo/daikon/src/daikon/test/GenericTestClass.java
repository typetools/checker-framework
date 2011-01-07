// This class is used in daikon.test.TestAst.

// You can append methods after the last one, but don't rearrange
// those already there.

package daikon.test;

import java.util.*;

public class GenericTestClass <A, B extends String, C extends java.lang.Object, U> {

  public List foo1() { return null; }

  public List<String> foo2() { return null; }

  public U foo3() { return null; }

  <D extends Comparable> List<String> foo4() { return null;}

  <E extends java.lang.Object> List<U> foo5() { return null;}

  <F> List<String> foo55() { return null;}

  public List foo6(List x) { return null; }

  public List foo7(List<A> x) { return null; }

  public List foo8(A x) { return null; }

  public List foo9(B x) { return null; }

  public List foo10(C x) { return null; }

  <G extends Comparable> List<U> foo11(G x, C y) { return null;}

  // shadowing
  <C extends Comparable> List<U> foo115(C x, B y) { return null;}

  <G extends Comparable> List<String> foo12(A x, List<B> y) { return null;}

  <G extends Comparable> List<String> foo13(A x, List<U> y) { return null;}

  <H extends java.lang.Object> List<String> foo14(H x) { return null;}

  <H extends java.lang.Object> List<U> foo15(B x) { return null;}

  <I> List<String> foo16(I x) { return null;}

  <I> List<String> foo17(I[] x) { return null;}

  <I> List<String> foo18(I[][] x) { return null;}

  <G extends Comparable> List<U> foo19(G[] x, C[] y) { return null;}

  // Ugh! But this is legal.
  List[] foo20(Comparable[][] x[], Object[] y[])[] { return null;}

  // This is not legal in Java 6.
  // public class Simple<U extends Map, V extends U.Entry> {
  //   public void foo1 (V x) { }
  //   public void foo2 (U.Entry x) { }
  // }

}
