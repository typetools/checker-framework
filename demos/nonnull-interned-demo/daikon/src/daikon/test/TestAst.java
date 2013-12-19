package daikon.test;

import daikon.PptName;
import daikon.tools.jtb.*;
import junit.framework.*;

import jtb.*;
import jtb.visitor.*;
import jtb.syntaxtree.*;

import java.io.*;
import java.util.*;

/**
 * Tests functionality of some methods in daikon.tools.jtb.Ast.
 *
 * TODO implement and test handling of "..." construct.
 */
public final class TestAst extends TestCase {

  public static void main(String[] args) {
    junit.textui.TestRunner.run(new TestSuite(TestAst.class));
  }

  public TestAst(String name) {
    super(name);
  }

  public static class MethodDeclarationHarvester extends DepthFirstVisitor {
    List<MethodDeclaration> decls = new ArrayList<MethodDeclaration>();
    public void visit(MethodDeclaration m) {

      decls.add(m);

      m.f0.accept(this);
      m.f1.accept(this);
      m.f2.accept(this);
      m.f3.accept(this);
      m.f4.accept(this);
    }
  }


  public static class ClassOrInterfaceDeclarationHarvester extends DepthFirstVisitor {
    List<ClassOrInterfaceDeclaration> decls = new ArrayList<ClassOrInterfaceDeclaration>();
    public void visit(ClassOrInterfaceDeclaration m) {
      decls.add(m);
      m.f0.accept(this);
      m.f1.accept(this);
      m.f2.accept(this);
      m.f3.accept(this);
      m.f4.accept(this);
      m.f5.accept(this);
    }
  }

  private void checkMatch(String pptNameString, MethodDeclaration decl, PptNameMatcher matcher) {

    PptName pptName = new PptName(pptNameString);
    boolean result = matcher.matches(pptName, decl);
    String declString = null;
    if (result == false) {
      // Format so we can print an error message.
      decl.accept(new TreeFormatter());
      declString = Ast.format(decl);
    }
    assertTrue("pptName: " + pptName +
               "\ndoesn't match method declaration:\n----------\n"
               + declString + "\n----------",
               result == true);
  }

  public void test_Ast_Ppt_Match() {

    // Parse the file "GenericTestClass.java" (under same dir as this class)
    InputStream sourceIn = this.getClass().getResourceAsStream("GenericTestClass.java");
    JavaParser parser = new JavaParser(sourceIn);

    CompilationUnit compilationUnit = null;

    try {
      compilationUnit = parser.CompilationUnit();
    } catch (ParseException e) {
      throw new Error(e);
    }


    // Test class declarations

    // Pick off class declarations
    ClassOrInterfaceDeclarationHarvester classDeclarationHarvester = new ClassOrInterfaceDeclarationHarvester();

    compilationUnit.accept(classDeclarationHarvester);
    List<ClassOrInterfaceDeclaration> classDecls = classDeclarationHarvester.decls;

    {
      String expected = "daikon.test.GenericTestClass";
      assertEquals("Got: " + classDecls.get(0) + "\nExpected: " + expected,
                   Ast.getClassName(classDecls.get(0)), expected);
    }
    // Illegal in Java 6
    // {
    //   String expected = "daikon.test.GenericTestClass.Simple";
    //   assertEquals("Got: " + classDecls.get(1) + "\nExpected: " + expected,
    //                Ast.getClassName(classDecls.get(1)), expected);
    // }

    // Test method declarations

    // Pick off method declarations
    MethodDeclarationHarvester methodDeclarationHarvester = new MethodDeclarationHarvester();

    compilationUnit.accept(methodDeclarationHarvester);

    List<MethodDeclaration> methodDecls = methodDeclarationHarvester.decls;

    PptNameMatcher matcher = new PptNameMatcher(compilationUnit);

    MethodDeclaration decl = null;

    decl = methodDecls.get(0);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo1");
    checkMatch("daikon.test.GenericTestClass.foo1():::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo1():::EXIT10", decl, matcher);

    decl = methodDecls.get(1);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo2");
    checkMatch("daikon.test.GenericTestClass.foo2():::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo2():::EXIT12", decl, matcher);

    decl = methodDecls.get(2);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo3");
    checkMatch("daikon.test.GenericTestClass.foo3():::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo3():::EXIT14", decl, matcher);

    decl = methodDecls.get(3);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo4");
    checkMatch("daikon.test.GenericTestClass.foo4():::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo4():::EXIT16", decl, matcher);

    decl = methodDecls.get(4);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo5");
    checkMatch("daikon.test.GenericTestClass.foo5():::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo5():::EXIT18", decl, matcher);

    decl = methodDecls.get(5);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo55");
    checkMatch("daikon.test.GenericTestClass.foo55():::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo55():::EXIT20", decl, matcher);

    decl = methodDecls.get(6);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo6");
    checkMatch("daikon.test.GenericTestClass.foo6(java.util.List):::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo6(java.util.List):::EXIT22", decl, matcher);

    decl = methodDecls.get(7);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo7");
    checkMatch("daikon.test.GenericTestClass.foo7(java.util.List):::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo7(java.util.List):::EXIT24", decl, matcher);

    decl = methodDecls.get(8);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo8");
    checkMatch("daikon.test.GenericTestClass.foo8(java.lang.Object):::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo8(java.lang.Object):::EXIT26", decl, matcher);

    decl = methodDecls.get(9);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo9");
    checkMatch("daikon.test.GenericTestClass.foo9(java.lang.String):::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo9(java.lang.String):::EXIT28", decl, matcher);

    decl = methodDecls.get(10);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo10");
    checkMatch("daikon.test.GenericTestClass.foo10(java.lang.Object):::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo10(java.lang.Object):::EXIT30", decl, matcher);

    decl = methodDecls.get(11);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo11");
    checkMatch("daikon.test.GenericTestClass.foo11(java.lang.Comparable, java.lang.Object):::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo11(java.lang.Comparable, java.lang.Object):::EXIT32", decl, matcher);

    decl = methodDecls.get(12);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo115");
    checkMatch("daikon.test.GenericTestClass.foo115(java.lang.Comparable, java.lang.String):::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo115(java.lang.Comparable, java.lang.String):::EXIT35", decl, matcher);

    decl = methodDecls.get(13);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo12");
    checkMatch("daikon.test.GenericTestClass.foo12(java.lang.Object, java.util.List):::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo12(java.lang.Object, java.util.List):::EXIT37", decl, matcher);

    decl = methodDecls.get(14);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo13");
    checkMatch("daikon.test.GenericTestClass.foo13(java.lang.Object, java.util.List):::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo13(java.lang.Object, java.util.List):::EXIT39", decl, matcher);

    decl = methodDecls.get(15);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo14");
    checkMatch("daikon.test.GenericTestClass.foo14(java.lang.Object):::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo14(java.lang.Object):::EXIT41", decl, matcher);

    decl = methodDecls.get(16);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo15");
    checkMatch("daikon.test.GenericTestClass.foo15(java.lang.String):::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo15(java.lang.String):::EXIT43", decl, matcher);

    decl = methodDecls.get(17);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo16");
    checkMatch("daikon.test.GenericTestClass.foo16(java.lang.Object):::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo16(java.lang.Object):::EXIT45", decl, matcher);

    decl = methodDecls.get(18);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo17");
    checkMatch("daikon.test.GenericTestClass.foo17(java.lang.Object[]):::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo17(java.lang.Object[]):::EXIT47", decl, matcher);

    decl = methodDecls.get(19);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo18");
    checkMatch("daikon.test.GenericTestClass.foo18(java.lang.Object[][]):::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo18(java.lang.Object[][]):::EXIT49", decl, matcher);

    decl = methodDecls.get(20);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo19");
    checkMatch("daikon.test.GenericTestClass.foo19(java.lang.Comparable[], java.lang.Object[]):::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo19(java.lang.Comparable[], java.lang.Object[]):::EXIT51", decl, matcher);

    decl = methodDecls.get(21);
    assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo20");
    checkMatch("daikon.test.GenericTestClass.foo20(java.lang.Comparable[][][], java.lang.Object[][]):::ENTER", decl, matcher);
    checkMatch("daikon.test.GenericTestClass.foo20(java.lang.Comparable[][][], java.lang.Object[][]):::EXIT53", decl, matcher);

    // Illegal in Java 6
    //
    // decl = methodDecls.get(22);
    // assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo1");
    // checkMatch("daikon.test.GenericTestClass.Simple.foo1(java.util.Map.Entry):::ENTER", decl, matcher);
    // checkMatch("daikon.test.GenericTestClass.Simple.foo1(java.util.Map.Entry):::EXIT12", decl, matcher);
    //
    // decl = methodDecls.get(23);
    // assertEquals(decl.f2.f0.tokenImage, decl.f2.f0.tokenImage, "foo2");
    // checkMatch("daikon.test.GenericTestClass.Simple.foo2(java.util.Map.Entry):::ENTER", decl, matcher);
    // checkMatch("daikon.test.GenericTestClass.Simple.foo2(java.util.Map.Entry):::EXIT14", decl, matcher);

  }
}
