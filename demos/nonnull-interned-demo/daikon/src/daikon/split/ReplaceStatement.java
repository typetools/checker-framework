package daikon.split;

import jtb.*;
import jtb.syntaxtree.*;
import jtb.visitor.*;
import daikon.tools.jtb.*;
import utilMDE.Assert;
import java.io.*;
import java.util.*;


/**
 * ReplaceStatement is an immutable data structure for holding
 * the information of a replace statement from a .spinfo file.
 * See "The Daikon Invariant Detector User Manual" for more information
 * on replace statements.
 */
class ReplaceStatement {

  /**
   * The method name of a replace statement. The method name
   * should not include prefixes such as "public" or "void".
   * However prefixes such as package names are allowed. For
   * example "somePackage.myMethod" is a valid method name for this
   * field.
   */
  private String methodName;

  /**
   * The method Parameters for the method of this replace statement.
   */
  private MethodParameter[] parameters;

  /**
   * The return statement of a replace statement. See constructor
   * for details of format.
   */
  private String returnStatement;

  /**
   * Creates a new instance of ReplaceStatement.
   * @param methodDeclaration the method declaration of this replace
   *  statement.  The method declaration should not include prefixes
   *  such as "public" or "void".  However, prefixes to the method name
   *  such as the package name are allowed. For example
   *  "somePrefix.myMethod(int x)" is a valid method declaration for
   *  this constructor.
   * @param returnStatement the return statement of a replace statement.
   *  The return statement should not include "return" at its beginning
   *  or ";" at its end. For example "x + 2" is a valid return statement
   *  for this field.
   * @throws IllegalArgumentException if methodDeclaration is not a valid
   *  java method declaration (with the exception of package name like
   *  prefixes.
   */
  public ReplaceStatement(String methodDeclaration, String returnStatement)
    throws ParseException {
    methodName = "";
    this.returnStatement = returnStatement;
    // Must remove any prefixes of the method name so that the java parser
    // can parse it correctly.
    int openParenIndex = methodDeclaration.indexOf('(');
    int dotIndex = methodDeclaration.lastIndexOf('.', openParenIndex);
    if (dotIndex != -1) {
      methodName = methodDeclaration.substring(0, dotIndex + 1);
      methodDeclaration = methodDeclaration.substring(dotIndex + 1);
    }
    String replaceClass = "class c { void " + methodDeclaration + "{}}";
    Reader input = new StringReader(replaceClass);
    JavaParser parser = new JavaParser(input);
    Node root = parser.CompilationUnit();
    MethodDeclarationParser visitor = new MethodDeclarationParser();
    parameters = null;          // to quiet FindBugs
    // Sets methodName and parameters.
    // But also seems to depend on methodName being set already...
    root.accept(visitor);
    Assert.assertTrue(methodName != null);
    Assert.assertTrue(parameters != null);
  }

  /**
   * Returns the name of method represented by this ReplaceStatement.
   */
  public String getName() {
    return methodName;
  }

  /**
   * Returns the return line of the method represented by this ReplaceStatement.
   */
  public String getReturnStatement() {
    return returnStatement;
  }

  /**
   * Returns the MethodParameters of the method represented by this ReplaceStatement.
   */
  public MethodParameter[] getParameters() {
    return parameters.clone();
  }

  /**
   * Returns a string representation of this.
   */
  public String toString() {
    StringBuffer params = new StringBuffer();
    for (int i = 0; i < parameters.length; i++) {
      params.append(parameters[i].toString());
    }
    return "<ReplaceStatement: " + methodName + "(" + params + "), " + returnStatement + ">";
  }

  /**
   * MethodDeclarationParser is a JTB syntax tree visitor for extracting
   * the name and arguments from a method declaration.  For example
   * from "someMethod(int x, int y)".  "someMethod", "int x", and "int y"
   * would be extracted.
   */
  private class MethodDeclarationParser extends DepthFirstVisitor {

    /**
     * Creates a new instance of MethodDeclarationParser.
     */
    private MethodDeclarationParser() {
      super();
    }

    /**
     * This method should not be directly used by users of this class.
     * Sets methodName, and parameters.
     */
    public void visit(MethodDeclaration n) {
      methodName = methodName + Ast.getName(n);
      List<MethodParameter> params = new ArrayList<MethodParameter>();
      List<FormalParameter> formalParameters = Ast.getParameters(n);
      for (FormalParameter fp : formalParameters) {
        String paramName = Ast.getName(fp);
        String paramType = Ast.getType(fp);
        MethodParameter methodParam = new MethodParameter(paramName, paramType);
        params.add(methodParam);
      }
      parameters = params.toArray(new MethodParameter[0]);
    }
  }

  /**
   * MethodParameter is a simple immutable ADT for representing
   * the name and type of a method parameter.
   */
  static public class MethodParameter {
    public String type;
    public String name;
    public MethodParameter(String name, String type) {
      this.name = name;
      this.type = type;
    }
  }

}
