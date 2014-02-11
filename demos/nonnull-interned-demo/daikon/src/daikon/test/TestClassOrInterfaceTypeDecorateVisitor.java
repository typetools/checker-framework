package daikon.test;

import daikon.PptName;
import daikon.tools.jtb.*;
import junit.framework.*;
import utilMDE.UtilMDE;

import jtb.*;
import jtb.visitor.*;
import jtb.syntaxtree.*;

import java.io.*;
import java.util.*;

public final class TestClassOrInterfaceTypeDecorateVisitor extends TestCase {

  public static void main(String[] args) {
    junit.textui.TestRunner.run(new TestSuite(TestClassOrInterfaceTypeDecorateVisitor.class));
  }

  public TestClassOrInterfaceTypeDecorateVisitor(String name) {
    super(name);
  }


  public static class UngenerifiedTypeCollector extends DepthFirstVisitor {
    List<ClassOrInterfaceType> generifieds = new ArrayList<ClassOrInterfaceType>();
    List<ClassOrInterfaceType> ungenerifieds = new ArrayList<ClassOrInterfaceType>();
    public void visit(ClassOrInterfaceType n) {
      generifieds.add(n);
      ungenerifieds.add(n.unGenerifiedVersionOfThis);
    }
    public String collectionResults() {
      StringBuffer b = new StringBuffer();
      b.append("Collection results:\n");
      for (int i = 0 ; i < generifieds.size() ; i++) {
        MethodDeclaration m = (MethodDeclaration)Ast.getParent(MethodDeclaration.class, generifieds.get(i));
        if (m != null) {
          b.append("Method: ");
          m.f0.accept(new TreeFormatter());
          b.append(Ast.format(m.f0));
          m.f1.accept(new TreeFormatter());
          b.append(Ast.format(m.f1));
          m.f2.accept(new TreeFormatter());
          b.append(Ast.format(m.f2));
          b.append("\n");
        }
        generifieds.get(i).accept(new TreeFormatter());
        b.append("  " + Ast.format(generifieds.get(i)));
        b.append("  -->");
        ungenerifieds.get(i).accept(new TreeFormatter());
        b.append("  " + Ast.format(ungenerifieds.get(i)));
        b.append("\n");
      }
      return b.toString();
    }
  }

  public void testTheVisitor() {

    // Parse the file "GenericTestClass.java" (under same dir as this class)
    InputStream sourceIn = this.getClass().getResourceAsStream("GenericTestClass.java");
    JavaParser parser = new JavaParser(sourceIn);

    CompilationUnit compilationUnit = null;

    try {
      compilationUnit = parser.CompilationUnit();
    } catch (ParseException e) {
      throw new Error(e);
    }

    UngenerifiedTypeCollector ungenerifiedCollector = new UngenerifiedTypeCollector();
    compilationUnit.accept(new ClassOrInterfaceTypeDecorateVisitor());
    compilationUnit.accept(ungenerifiedCollector);
    


    /*
     for (int ii = 0; ii < result.length(); ii++) {
      if (result.charAt(ii) !=  expected.charAt(ii)) {
        System.out.printf ("diff at offset %d: '%c' - '%c'%n", ii, 
                           result.charAt(ii), expected.charAt(ii));
        System.out.printf ("last:%n%s%n%s%n", result.substring (ii-50, ii+2),
                           expected.substring (ii-50, ii+2));
        break;
      }
    }
    */

    String result = ungenerifiedCollector.collectionResults().trim();
    String[] result_arr = UtilMDE.splitLines (result);
    String expected = expectedAnswerBuffer.toString().trim();
    String[] expected_arr = UtilMDE.splitLines (expected);

    // UtilMDE.writeFile (new File ("expected.txt"), expected);
    // UtilMDE.writeFile (new File ("result.txt"), result);

    assertEquals ("diff in buffer lengths", expected_arr.length, 
                  result_arr.length);
    for (int ii = 0; ii < expected_arr.length; ii++) {
      Assert.assertEquals ("diff at line " + ii, expected_arr[ii], 
                           result_arr[ii]);
    }
      /*
    Assert.assertTrue(ungenerifiedCollector.collectionResults()
                      + "\n\n\nExpected answer:\n\n\n"
                      + expectedAnswerBuffer.toString(),
                      ungenerifiedCollector.collectionResults().trim().equals(expectedAnswerBuffer.toString().trim()));
      */
  }

  private static StringBuffer expectedAnswerBuffer = new StringBuffer();
  private static final String lineSep = System.getProperty("line.separator");

  static {
    expectedAnswerBuffer.append("Collection results:\n");
    expectedAnswerBuffer.append("  String  -->  String\n");
    expectedAnswerBuffer.append("  java.lang.Object  -->  java.lang.Object\n");
    expectedAnswerBuffer.append("Method: Listfoo1()\n");
    expectedAnswerBuffer.append("  List  -->  List\n");
    expectedAnswerBuffer.append("Method: List<String>foo2()\n");
    expectedAnswerBuffer.append("  List<String>  -->  List\n");
    expectedAnswerBuffer.append("Method: Ufoo3()\n");
    expectedAnswerBuffer.append("  U  -->  Object\n");
    expectedAnswerBuffer.append("Method: <D extends Comparable >List<String>foo4()\n");
    expectedAnswerBuffer.append("  Comparable  -->  Comparable\n");
    expectedAnswerBuffer.append("Method: <D extends Comparable >List<String>foo4()\n");
    expectedAnswerBuffer.append("  List<String>  -->  List\n");
    expectedAnswerBuffer.append("Method: <E extends java.lang.Object >List<U>foo5()\n");
    expectedAnswerBuffer.append("  java.lang.Object  -->  java.lang.Object\n");
    expectedAnswerBuffer.append("Method: <E extends java.lang.Object >List<U>foo5()\n");
    expectedAnswerBuffer.append("  List<U>  -->  List\n");
    expectedAnswerBuffer.append("Method: <F >List<String>foo55()\n");
    expectedAnswerBuffer.append("  List<String>  -->  List\n");
    expectedAnswerBuffer.append("Method: Listfoo6(List x)\n");
    expectedAnswerBuffer.append("  List  -->  List\n");
    expectedAnswerBuffer.append("Method: Listfoo6(List x)\n");
    expectedAnswerBuffer.append("  List  -->  List\n");
    expectedAnswerBuffer.append("Method: Listfoo7(List<A> x)\n");
    expectedAnswerBuffer.append("  List  -->  List\n");
    expectedAnswerBuffer.append("Method: Listfoo7(List<A> x)\n");
    expectedAnswerBuffer.append("  List<A>  -->  List\n");
    expectedAnswerBuffer.append("Method: Listfoo8(A x)\n");
    expectedAnswerBuffer.append("  List  -->  List\n");
    expectedAnswerBuffer.append("Method: Listfoo8(A x)\n");
    expectedAnswerBuffer.append("  A  -->  Object\n");
    expectedAnswerBuffer.append("Method: Listfoo9(B x)\n");
    expectedAnswerBuffer.append("  List  -->  List\n");
    expectedAnswerBuffer.append("Method: Listfoo9(B x)\n");
    expectedAnswerBuffer.append("  B  -->  String\n");
    expectedAnswerBuffer.append("Method: Listfoo10(C x)\n");
    expectedAnswerBuffer.append("  List  -->  List\n");
    expectedAnswerBuffer.append("Method: Listfoo10(C x)\n");
    expectedAnswerBuffer.append("  C  -->  java.lang.Object\n");
    expectedAnswerBuffer.append("Method: <G extends Comparable >List<U>foo11(G x, C y)\n");
    expectedAnswerBuffer.append("  Comparable  -->  Comparable\n");
    expectedAnswerBuffer.append("Method: <G extends Comparable >List<U>foo11(G x, C y)\n");
    expectedAnswerBuffer.append("  List<U>  -->  List\n");
    expectedAnswerBuffer.append("Method: <G extends Comparable >List<U>foo11(G x, C y)\n");
    expectedAnswerBuffer.append("  G  -->  Comparable\n");
    expectedAnswerBuffer.append("Method: <G extends Comparable >List<U>foo11(G x, C y)\n");
    expectedAnswerBuffer.append("  C  -->  java.lang.Object\n");
    expectedAnswerBuffer.append("Method: // shadowing//\n");
    expectedAnswerBuffer.append("/* */\n");
    expectedAnswerBuffer.append("<C extends Comparable >List<U>foo115(C x, B y)\n");
    expectedAnswerBuffer.append("  Comparable  -->  Comparable\n");
    expectedAnswerBuffer.append("Method: // shadowing//\n");
    expectedAnswerBuffer.append("/* */\n");
    expectedAnswerBuffer.append("<C extends Comparable >List<U>foo115(C x, B y)\n");
    expectedAnswerBuffer.append("  List<U>  -->  List\n");
    expectedAnswerBuffer.append("Method: // shadowing//\n");
    expectedAnswerBuffer.append("/* */\n");
    expectedAnswerBuffer.append("<C extends Comparable >List<U>foo115(C x, B y)\n");
    expectedAnswerBuffer.append("  C  -->  Comparable\n");
    expectedAnswerBuffer.append("Method: // shadowing//\n");
    expectedAnswerBuffer.append("/* */\n");
    expectedAnswerBuffer.append("<C extends Comparable >List<U>foo115(C x, B y)\n");
    expectedAnswerBuffer.append("  B  -->  String\n");
    expectedAnswerBuffer.append("Method: <G extends Comparable >List<String>foo12(A x, List<B> y)\n");
    expectedAnswerBuffer.append("  Comparable  -->  Comparable\n");
    expectedAnswerBuffer.append("Method: <G extends Comparable >List<String>foo12(A x, List<B> y)\n");
    expectedAnswerBuffer.append("  List<String>  -->  List\n");
    expectedAnswerBuffer.append("Method: <G extends Comparable >List<String>foo12(A x, List<B> y)\n");
    expectedAnswerBuffer.append("  A  -->  Object\n");
    expectedAnswerBuffer.append("Method: <G extends Comparable >List<String>foo12(A x, List<B> y)\n");
    expectedAnswerBuffer.append("  List<B>  -->  List\n");
    expectedAnswerBuffer.append("Method: <G extends Comparable >List<String>foo13(A x, List<U> y)\n");
    expectedAnswerBuffer.append("  Comparable  -->  Comparable\n");
    expectedAnswerBuffer.append("Method: <G extends Comparable >List<String>foo13(A x, List<U> y)\n");
    expectedAnswerBuffer.append("  List<String>  -->  List\n");
    expectedAnswerBuffer.append("Method: <G extends Comparable >List<String>foo13(A x, List<U> y)\n");
    expectedAnswerBuffer.append("  A  -->  Object\n");
    expectedAnswerBuffer.append("Method: <G extends Comparable >List<String>foo13(A x, List<U> y)\n");
    expectedAnswerBuffer.append("  List<U>  -->  List\n");
    expectedAnswerBuffer.append("Method: <H extends java.lang.Object >List<String>foo14(H x)\n");
    expectedAnswerBuffer.append("  java.lang.Object  -->  java.lang.Object\n");
    expectedAnswerBuffer.append("Method: <H extends java.lang.Object >List<String>foo14(H x)\n");
    expectedAnswerBuffer.append("  List<String>  -->  List\n");
    expectedAnswerBuffer.append("Method: <H extends java.lang.Object >List<String>foo14(H x)\n");
    expectedAnswerBuffer.append("  H  -->  java.lang.Object\n");
    expectedAnswerBuffer.append("Method: <H extends java.lang.Object >List<U>foo15(B x)\n");
    expectedAnswerBuffer.append("  java.lang.Object  -->  java.lang.Object\n");
    expectedAnswerBuffer.append("Method: <H extends java.lang.Object >List<U>foo15(B x)\n");
    expectedAnswerBuffer.append("  List<U>  -->  List\n");
    expectedAnswerBuffer.append("Method: <H extends java.lang.Object >List<U>foo15(B x)\n");
    expectedAnswerBuffer.append("  B  -->  String\n");
    expectedAnswerBuffer.append("Method: <I >List<String>foo16(I x)\n");
    expectedAnswerBuffer.append("  List<String>  -->  List\n");
    expectedAnswerBuffer.append("Method: <I >List<String>foo16(I x)\n");
    expectedAnswerBuffer.append("  I  -->  Object\n");
    expectedAnswerBuffer.append("Method: <I >List<String>foo17(I[] x)\n");
    expectedAnswerBuffer.append("  List<String>  -->  List\n");
    expectedAnswerBuffer.append("Method: <I >List<String>foo17(I[] x)\n");
    expectedAnswerBuffer.append("  I  -->  Object\n");
    expectedAnswerBuffer.append("Method: <I >List<String>foo18(I[][] x)\n");
    expectedAnswerBuffer.append("  List<String>  -->  List\n");
    expectedAnswerBuffer.append("Method: <I >List<String>foo18(I[][] x)\n");
    expectedAnswerBuffer.append("  I  -->  Object\n");
    expectedAnswerBuffer.append("Method: <G extends Comparable >List<U>foo19(G[] x, C[] y)\n");
    expectedAnswerBuffer.append("  Comparable  -->  Comparable\n");
    expectedAnswerBuffer.append("Method: <G extends Comparable >List<U>foo19(G[] x, C[] y)\n");
    expectedAnswerBuffer.append("  List<U>  -->  List\n");
    expectedAnswerBuffer.append("Method: <G extends Comparable >List<U>foo19(G[] x, C[] y)\n");
    expectedAnswerBuffer.append("  G  -->  Comparable\n");
    expectedAnswerBuffer.append("Method: <G extends Comparable >List<U>foo19(G[] x, C[] y)\n");
    expectedAnswerBuffer.append("  C  -->  java.lang.Object\n");
    expectedAnswerBuffer.append("Method: // Ugh! But this is legal.//\n");
    expectedAnswerBuffer.append("/* */\n");
    expectedAnswerBuffer.append("List[]foo20(Comparable[][] x[], Object[] y[])[]\n");
    expectedAnswerBuffer.append("  // Ugh! But this is legal.//\n");
    expectedAnswerBuffer.append("/* */\n");
    expectedAnswerBuffer.append("List  -->  // Ugh! But this is legal.//\n");
    expectedAnswerBuffer.append("/* */\n");
    expectedAnswerBuffer.append("List\n");
    expectedAnswerBuffer.append("Method: // Ugh! But this is legal.//\n");
    expectedAnswerBuffer.append("/* */\n");
    expectedAnswerBuffer.append("List[]foo20(Comparable[][] x[], Object[] y[])[]\n");
    expectedAnswerBuffer.append("  Comparable  -->  Comparable\n");
    expectedAnswerBuffer.append("Method: // Ugh! But this is legal.//\n");
    expectedAnswerBuffer.append("/* */\n");
    expectedAnswerBuffer.append("List[]foo20(Comparable[][] x[], Object[] y[])[]\n");
    expectedAnswerBuffer.append("  Object  -->  Object\n");
    // This is illegal in Java 6.
    // expectedAnswerBuffer.append("  Map  -->  Map\n");
    // expectedAnswerBuffer.append("  U.Entry  -->  Map.Entry\n");
    // expectedAnswerBuffer.append("Method: voidfoo1(V x)\n");
    // expectedAnswerBuffer.append("  V  -->  Map.Entry\n");
    // expectedAnswerBuffer.append("Method: voidfoo2(U.Entry x)\n");
    // expectedAnswerBuffer.append("  U.Entry  -->  Map.Entry\n");
    expectedAnswerBuffer.append("\n");

  }

}
