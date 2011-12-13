package daikon.tools.jtb;

import java.util.*;
import jtb.syntaxtree.*;
import jtb.visitor.*;

// CreateSpinfo extracts the following expressions from the Java source:
// For each method:
//  * extracts all expressions in conditional statements,
//    ie. if, for, which, etc.
//  * if the method body is a one-line return statement, it
//    extracts the expression for later substitution into expressions which
//    call this function. These statements are referred to as
//    replace statements
// For each field declaration
//  * if the field is a boolean, it stores the expression
//    "<fieldname> == true" as a splitting condition.
//
//  The method printSpinfoFile prints out these expressions and
//  replace statements in splitter info file format.

class ConditionExtractor extends DepthFirstVisitor {

  private String packageName;
  private String className; // The class name.
  private String curMethodName; // Name of current method being parsed
  private String curMethodDeclaration;
  boolean enterMethod;   // true if the current Node is a Method
                         // declaration ie. we just entered a method.

  // Contains the resultType of the current method.  If the current method is a
  // constructor then the string "constructor" is stored. These is later used
  // to decide whether the return statement should be included as a conditional.
  // Return statements are included as conditionals iff the return type is "boolean"
  // Must be a stack rather than a single variable for the case of helper classes.
  private Stack<Object> resultTypes = new Stack<Object>(); // elements are ResultType or String

  // key = methodname (as String); value = conditional expressions (as Strings)
  HashMap<String,List<String>> conditions = new HashMap<String,List<String>>();
  // key = method declaration (String); value = method bodies (String)
  HashMap<String,String> replaceStatements = new HashMap<String,String>();


  //// DepthFirstVisitor Methods overridden by ConditionExtractor //////////////
  /////

  /**
   * f0 -> "package"
   * f1 -> Name()
   * f2 -> ";"
   */
  public void visit(PackageDeclaration n) {
    packageName = Ast.format(n.f1);
    super.visit(n);
  }

  /**
   * f0 -> "class"
   * f1 -> <IDENTIFIER>
   * f2 -> [ "extends" Name() ]
   * f3 -> [ "implements" NameList() ]
   * f4 -> ClassBody()
   */
  public void visit(ClassOrInterfaceDeclaration n) {

    if (!Ast.isInterface(n)) { // Not sure if this is needed; added during JTB udpate.
      className = Ast.format(n.f1);
    }
    super.visit(n);
  }

  /**
   * Stores the field name, if it is a boolean.
   */
  public void visit(FieldDeclaration n) {
    /**
     * Grammar production:
     * f0 -> Type()
     * f1 -> VariableDeclarator()
     * f2 -> ( "," VariableDeclarator() )*
     * f3 -> ";"
     */

    String resultType = Ast.format(n.f0);
    if (resultType.equals("boolean")) {
      addCondition(Ast.format(n.f1.f0) + " ==  true");  // <--
    }
    super.visit(n);
  }

  /**
   * f0 -> ( "public" | "protected" | "private" | "static" | "abstract" | "final" | "native" | "synchronized" )*
   * f1 -> ResultType()
   * f2 -> MethodDeclarator()
   * f3 -> [ "throws" NameList() ]
   * f4 -> ( Block() | ";" )
   */

  /**
   * It is sometimes helpful to store the method bodies of one-liner
   * methods.  They are useful as 'replace' statements when the
   * condition makes a call to that function. Here we keep track of
   * the fact that we have reached a method declaration.
   */
  public void visit(MethodDeclaration n) {
    // after the next non-empty statement, the variable
    // enterMethod is set to false
    enterMethod = true;
    resultTypes.push(n.f1);
    super.visit(n);
    resultTypes.pop();
  }

  /**
   * f0 -> <IDENTIFIER>
   * f1 -> FormalParameters()
   * f2 -> ( "[" "]" )*
   */
  public void visit(MethodDeclarator n) {
    // This goes on the PPT_NAME line of the spinfo file.
    // eg. QueueAr.isEmpty
    curMethodName = className + "." + Ast.format(n.f0);
    addMethod (Ast.format(n), curMethodName);
    super.visit(n);
  }

  /**
   * f0 -> [ "public" | "protected" | "private" ]
   * f1 -> <IDENTIFIER>
   * f2 -> FormalParameters()
   * f3 -> [ "throws" NameList() ]
   * f4 -> "{"
   * f5 -> [ ExplicitConstructorInvocation() ]
   * f6 -> ( BlockStatement() )*
   * f7 -> "}"
   */
  public void visit(ConstructorDeclaration n) {
    // This goes on the PPT_NAME line of the spinfo file.
    // eg. QueueAr.isEmpty
    curMethodName = className + "." + Ast.format(n.f1);
    addMethod(className + Ast.format(n), curMethodName);
    resultTypes.push("constructor");
    super.visit(n);
    resultTypes.pop();
  }

  /**
   * f0 -> "switch"
   * f1 -> "("
   * f2 -> Expression()
   * f3 -> ")"
   * f4 -> "{"
   * f5 -> ( SwitchLabel() ( BlockStatement() )* )*
   * f6 -> "}"
   */

  /**
   * extracts the values for the different cases and creates splitting
   * conditions out of them
   */
  public void visit(SwitchStatement n) {
    String switchExpression = Ast.format(n.f2);
    Collection<String> caseValues = getCaseValues(n.f5);
     // a condition for the default case. A 'not' of all the different cases.
    StringBuffer defaultString = new StringBuffer();
    for (String switchValue : caseValues) {
      switchValue = switchValue.trim();
      if (!switchValue.equals(":")) {
	if (!(defaultString.length() == 0))
	  defaultString.append(" && ");
	defaultString.append(switchExpression + " != " + switchValue);
	addCondition(switchExpression + " == " + switchValue);
      }
    }
    addCondition(defaultString.toString());
    super.visit(n);
  }

  /**
   * @return a String[] which contains the different Integer values
   * which the case expression is tested against
   */
  public Collection<String> getCaseValues (NodeListOptional n) {
    ArrayList<String> values = new ArrayList<String>();
    Enumeration e = n.elements();
    while (e.hasMoreElements()) {
      // in the nodeSequence for the switch statement,
      // the first element is always the case statement.
      // ie: case <value>: expr(); break;  | default ...
      NodeSequence ns = (NodeSequence) e.nextElement();
      SwitchLabel sl = (SwitchLabel) ns.elementAt(0);
      ns = (NodeSequence) sl.f0.choice;
      values.add(Ast.format(ns.elementAt(1)));
    }
    return values;
  }

  /**
   * f0 -> "if"
   * f1 -> "("
   * f2 -> Expression()
   * f3 -> ")"
   * f4 -> Statement()
   * f5 -> [ "else" Statement() ]
   */
  /* Extract the condition in an 'if' statement */
  public void visit(IfStatement n) {
    addCondition(Ast.format(n.f2));
    super.visit(n);
}

  /**
   * f0 -> "while"
   * f1 -> "("
   * f2 -> Expression()
   * f3 -> ")"
   * f4 -> Statement()
   */
  /* Extract the condition in an 'while' statement */
  public void visit(WhileStatement n) {
    super.visit(n);
    addCondition(Ast.format(n.f2));
  }

  /**
   * f0 -> "do"
   * f1 -> Statement()
   * f2 -> "while"
   * f3 -> "("
   * f4 -> Expression()
   * f5 -> ")"
   * f6 -> ";"
   */
  /* Extract the condition in an 'DoStatement' statement */
  public void visit(DoStatement n) {
    super.visit(n);
    addCondition(Ast.format(n.f4));
  }

   /**
    * f0 -> "for"
    * f1 -> "("
    * f2 -> ( Type() <IDENTIFIER> ":" Expression() | [ ForInit() ] ";" [ Expression() ] ";" [ ForUpdate() ] )
    * f3 -> ")"
    * f4 -> Statement()
    */
  /* Extract the condition in an 'for' statement */
  public void visit(ForStatement n) {
    super.visit(n);
    if (n.f2.which == 1) {
      addCondition(Ast.format(((NodeSequence)n.f2.choice).elementAt(2)));
    }

  }


  /**
   * f0 -> LabeledStatement()
   *       | Block()
   *       | EmptyStatement()
   *       | StatementExpression() ";"
   *       | SwitchStatement()
   *       | IfStatement()
   *       | WhileStatement()
   *       | DoStatement()
   *       | ForStatement()
   *       | BreakStatement()
   *       | ContinueStatement()
   *       | ReturnStatement()
   *       | ThrowStatement()
   *       | SynchronizedStatement()
   *       | TryStatement()
   */

  /*
   * If this statement is a one-liner (the sole statement in a
   * function), then it is saved and used as a 'replace' statement in
   * the splitter info file.
   *
   * If this statement is a return statement of boolean type, then
   * it is included as a condition.
   */
  public void visit(Statement n) {
    // if we just entered the function and this is a return statement,
    // then it's a one-liner. Save the statement
    if (n.f0.choice instanceof ReturnStatement) {
      ReturnStatement rs = ((ReturnStatement) n.f0.choice);
      String returnExpression = Ast.format(rs.f1);
      if (enterMethod) {
        addReplaceStatement( returnExpression );
        enterMethod = false;
      }
      if (resultTypes.peek() instanceof ResultType) {
        ResultType resultType = (ResultType) resultTypes.peek();
        if (resultType.f0.choice instanceof Type) {
          Type type = (Type) resultType.f0.choice;
          if (Ast.isPrimitive(type)) {
            PrimitiveType primType = (PrimitiveType) type.f0.choice;
            if (((NodeToken) primType.f0.choice).toString().equals("boolean")) {
              addCondition(returnExpression);
            }
          }
        }
      }
    } else if (!(n.f0.choice instanceof EmptyStatement)) {
      enterMethod = false;
    }
    super.visit(n);
  }

  //////// Private methods specific to ConditionExtractor ////
  /**
   * Keep track of the method we are currently in, and create an entry
   * for it, so that the conditions can be associated with the right
   * methods.
   */
  private void addMethod(String methodDeclaration, String methodname) {
    curMethodName = methodname;
    curMethodDeclaration = methodDeclaration;
  }

  // add the condition to the current method's list of conditions
  private void addCondition(String cond) {
    String meth = curMethodName;
    if (meth == null) {
      meth = className + ":::OBJECT";
    }
    if (! conditions.containsKey(meth)) {
      conditions.put(meth, new Vector<String>());
    }
    List<String> conds = conditions.get(meth);
    conds.add(cond);
  }

  // store the replace statement (expression) in the hashmap
  private void addReplaceStatement(String s) {
    if (curMethodDeclaration != null) {
      replaceStatements.put(curMethodDeclaration, s);
    }
  }

   public Map<String,List<String>> getConditionMap() {
     return conditions;
   }

  public Map<String,String> getReplaceStatements() {
    return replaceStatements;
  }

  public String getPackageName() {
    return packageName;
  }

}
