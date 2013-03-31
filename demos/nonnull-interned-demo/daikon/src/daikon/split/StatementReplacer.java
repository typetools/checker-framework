package daikon.split;


import java.util.*;
import jtb.syntaxtree.*;
import jtb.visitor.*;
import jtb.ParseException;
import daikon.tools.jtb.*;


/**
 * StatementReplacer is a jtb syntax tree visitor that replaces method calls
 * to one line methods with their bodies.  The bodies of the methods
 * have their variable names changed to the correct argument names.
 * Replacer applies it self repeatedly such that if the body of the one
 * liner calls a one liner, it will make both replacements.
 * For example, take the methods
 * "int someMethod(int x, int y) { return  x + anotherMethod(y + 1); }" and
 * "int anotherMethod(int z) { return 2*z + 5; }"
 * a call to "makeReplacements(someMethod(a/3, b))" would yield:
 * "(a/3) + 2*((b) + 1) + 5"
 *
 * Once made from ReplaceStatements, a replacer can be used on a series of
 * statements.
 */
class StatementReplacer extends DepthFirstVisitor {

  /** A ReplaceStatementMap for the ReplaceStatements of this. */
  private ReplaceStatementMap statementMap;

  /**
   * true iff a match between the method name in
   * the PrimaryExpression currently being visited and one
   * of the members of methodNames is found.
   */
  private boolean matchFound;

  /**
   * Creates a new instance of StatementReplacer that makes the
   * replacements specified by the ReplaceStatements of replaceStatements.
   * @param replaceStatements a list of ReplaceStatements specifying the
   *  replacements to be made by this.
   */
  public StatementReplacer(List<ReplaceStatement> replaceStatements) {
    statementMap = new ReplaceStatementMap(replaceStatements);
  }

  static final int MAXREPLACEMENTS = 10;

  /**
   * Makes the replacements in statement that are designated by this.
   * See class description for details.
   * @param expression a segment of valid java code in which the
   *  the replacements should be made.
   * @return statement with the correct replacements made.
   */
  public String makeReplacements(String expression) throws ParseException {
    // originalExpression and replacements detect loops.  Gross.
    String originalExpression = expression;
    int replacements = 0;
    String replacedExpression = expression;
    do {
      expression = replacedExpression;
      // expression must be re-parsed with every loop because the
      // syntax tree is made invalid when visited by this.
      // TODO: This removes spaces, turning "a instanceof B" into "ainstanceofB"
      // System.out.println("about to call getJtbTree on: " + expression);
      Node root = Visitors.getJtbTree(expression);
      try {
        root.accept(this);
        replacedExpression = Ast.format(root);
        replacements++;
      } catch (IllegalStateException e) {
        // ParseException does not accept optional "cause" argument
        throw new ParseException(e.getMessage());
      }
    } while ((replacements < MAXREPLACEMENTS)
             && (! replacedExpression.equals(expression)));
    if (replacements >= MAXREPLACEMENTS) {
      return originalExpression;
    } else {
      // System.out.println("makeReplacements(" + originalExpression + ") ==> " + replacedExpression);
      return replacedExpression;
    }
  }

  /**
   * This method should not be used directly by users of this class;
   * however, it must be public to full-fill the visitor interface.
   * If n is a method call with a replacement, then the variables
   * of the replacement statement are replaced by the arguments
   * to the method call and then the replacement statement is
   * substituted for the method call.  The first token of n is set
   * to the replace statement.  All the other tokens are set to the
   * empty string by visit(NodeToken n).
   * @param n the possible method call in which replacement may be
   *  made.
   */
  public void visit(PrimaryExpression n) {
    if (! matchFound) {
      ReplaceStatement replaceStatement = null;
      NodeToken firstToken = null;
      List<String> newArgs = null;
      if (isNonThisMethod(n)) {
         replaceStatement = statementMap.get(getNonThisName(n));
         firstToken = ((Name) n.f0.f0.choice).f0;
         newArgs = getNonThisArgs(n);
      } else if (isThisDotMethod(n)) {
        replaceStatement = statementMap.get(getThisName(n));
        firstToken = (NodeToken) n.f0.f0.choice;
        newArgs = getArgs(n);
      }
      if (replaceStatement != null && firstToken != null) {
        List<String> oldArgs = getParameterNames(replaceStatement.getParameters());
        String newReturnStatement;
        try {
          newReturnStatement =
           TokenReplacer.replaceTokens(replaceStatement.getReturnStatement(),
                                        oldArgs,
                                        newArgs);
          matchFound = true;
          super.visit(n);
          matchFound = false;
          firstToken.tokenImage = newReturnStatement;
          return;
        } catch (ParseException e) {
         // need to throw an unchecked Exception since visit
         // cannot throw a checked Exception.
         throw new IllegalStateException(e.getMessage(), e);
        }
      }
    }
    super.visit(n);
  }

  /**
   * Returns a List of the parameter names (as Strings) of the
   * MethodParameters of params.
   * @param params the MethodParameters' whose names are desired.
   */
  private List<String> getParameterNames(ReplaceStatement.MethodParameter[] params) {
    List<String> args = new ArrayList<String>();
    for (int i = 0; i < params.length; i++) {
      args.add(params[i].name);
    }
    return args;
  }

  /**
   * This method should not be used directly by users of this class;
   * however, it must be public to full-fill the visitor interface.
   * Sets all tokens except the first in a Primary expression to the
   * empty string. All begin columns and endColumns are set to -1, to
   * ensure that Ast printing exceptions are not thrown.
   */
  public void visit(NodeToken n) {
    if (matchFound) {
      n.tokenImage = "";
    }
    n.beginColumn = -1;
    n.endColumn = -1;
    super.visit(n);
  }

  /**
   * Returns the name of the method call represented by n.
   * @param n a "non-this" method call.
   */
  private String getNonThisName(PrimaryExpression n) {
    Name nameNode = (Name) n.f0.f0.choice;
    return Ast.format(nameNode);
  }

  /**
   * Returns the name of the method call represented by n including
   * the "this." prefix.
   * @param n a "this" method call.
   */
  private String getThisName(PrimaryExpression n) {
    return Ast.format(n.f0) + Ast.format(n.f1.elementAt(0));
  }

  /**
   * Returns whether n represents a "non-this" call to a method.
   * "Non-this" methods calls are not prefixed with "this.".
   * For example "get(5)" and "Collections.sort(new ArrayList())"
   * are "non-this" method calls.
   */
  private boolean isNonThisMethod(PrimaryExpression n) {
    return (n.f0.f0.choice instanceof Name &&
            n.f1.size() > 0 &&
            n.f1.elementAt(0) instanceof PrimarySuffix &&
            ((PrimarySuffix) n.f1.elementAt(0)).f0.choice instanceof Arguments);
  }

  /**
   * Returns whether n represents a "this" call to a method.
   * "This" methods calls are prefixed with "this.".
   * For example "this.get(5)" is a "this" method call.
   */
  private boolean isThisDotMethod(PrimaryExpression n) {
    return (n.f0.f0.choice instanceof NodeToken &&
            Visitors.isThis((NodeToken) n.f0.f0.choice) &&
            n.f1.size() == 2 &&
            n.f1.elementAt(1) instanceof PrimarySuffix &&
            ((PrimarySuffix) n.f1.elementAt(1)).f0.choice instanceof Arguments);
  }

  /**
   * Returns the arguments from the "this" method call n. For example
   * "method(x, y + 1)" would yield "[(x), (y + 1)]"
   * @param n the "this" method call from which the arguments should be extracted.
   * @return a list of arguments from the method call n.
   */
  private List<String> getArgs(PrimaryExpression n) {
    List<String> args = new ArrayList<String>();
    int index = n.f1.size()- 1;
    if (index > 1) {
     index = 1;
    }
    Arguments argumentNode =
      (Arguments) ((PrimarySuffix) n.f1.elementAt(index)).f0.choice;
    if (argumentNode.f1.present()) {
      ArgumentList argListNode = (ArgumentList) argumentNode.f1.node;
      args.add(addParens(Ast.format(argListNode.f0)));
      if (argListNode.f1.present()) {
        NodeListOptional additionalArgsNode = argListNode.f1;
        for (int i = 0; i < additionalArgsNode.size(); i++) {
          args.add(addParens(Ast.format(additionalArgsNode.elementAt(i))));
        }
      }
    }
    return args;
  }

  /**
   * Returns the arguments from the "non-this" method call n. For example
   * "method(x, y)" would yield "[x, y]"
   * @param n the "non-this" method call from which the arguments should be extracted.
   * @return a list of arguments from the method call n.
   */
  private List<String> getNonThisArgs(PrimaryExpression n) {
    List<String> args = new ArrayList<String>();
    int index = 0;
    Arguments argumentNode =
      (Arguments) ((PrimarySuffix) n.f1.elementAt(index)).f0.choice;
    if (argumentNode.f1.present()) {
      ArgumentList argListNode = (ArgumentList) argumentNode.f1.node;
      args.add(Ast.format(argListNode.f0));
      if (argListNode.f1.present()) {
        NodeListOptional additionalArgsNode = argListNode.f1;
        for (int i = 0; i < additionalArgsNode.size(); i++) {
          args.add(Ast.format(additionalArgsNode.elementAt(i)));
        }
      }
    }
    return args;
  }

  /**
   * Returns the argument with parens placed around it unless there
   * are already parens the argument.  For example, "x" would yield
   * "(x)", "x + 1" would yeild "(x + 1)", and "(x+1)" would yield
   * no change.
   */
  public static String addParens(String arg) {
    arg = arg.trim();
    if (arg.charAt(0) == '(' && arg.charAt(arg.length() -1) == ')') {
      return arg;
    }
    return "(" + arg + ")";
  }

}
