package daikon.tools.jtb;

import java.util.*;
import java.io.*;
import jtb.*;
import jtb.syntaxtree.*;
import jtb.visitor.*;
import utilMDE.*;
import daikon.Global;

/**
 * Given a class with certain methods annotated as observers, adds one
 * field for each method, and adds code to update those fields with
 * the value of the observer at every procedure entry and exit.
 *
 * <b>Be careful</b> with this tool, as it overwrites the files passed
 * on the command line!
 **/
public class InstrumentObserversVisitor
  extends DepthFirstVisitor
{

  /** @param observers a collection of MethodDeclarations **/
  public InstrumentObserversVisitor(Collection<MethodDeclaration> observers) {
    observer_methods = new ArrayList<MethodDeclaration>(observers);
  }

  // All methods that we want to observe
  private final List<MethodDeclaration> observer_methods;

  /**
   * Add fields for each observed method.
   **/
  public void visit(ClassOrInterfaceBody clazzOrInterface) {
    super.visit(clazzOrInterface);

    if (Ast.isInterface(clazzOrInterface)) {
      return;
    }

    for (MethodDeclaration method : observer_methods) {
      String fldtype = getFieldTypeFor(method);
      String fldname = getFieldNameFor(method);
      String code = "private " + fldtype + " " + fldname + ";" + Global.lineSep;
      ClassOrInterfaceBodyDeclaration decl =
        (ClassOrInterfaceBodyDeclaration) Ast.create("ClassOrInterfaceBodyDeclaration", code);
      Ast.addDeclaration(clazzOrInterface, decl);
    }

    StringBuffer code = new StringBuffer();
    code.append("private void daikon_update() {");
    code.append("  if (daikon.Runtime.ps_count == 0) {");
    code.append("    daikon.Runtime.ps_count++;");
    for (MethodDeclaration obs_method : observer_methods) {
      String fldname = getFieldNameFor(obs_method);
      code.append("    " + fldname + " = " + getCallExprFor(obs_method) + ";");
    }
    code.append("    daikon.Runtime.ps_count--;");
    code.append("  }");
    code.append("}");

    ClassOrInterfaceBodyDeclaration decl = (ClassOrInterfaceBodyDeclaration)
      Ast.create("ClassOrInterfaceBodyDeclaration", code.toString());
    Ast.addDeclaration(clazzOrInterface, decl);
  }

  /**
   * Add a call to daikon_update at the end of the constructor.
   **/
  public void visit(ConstructorDeclaration ctor) {
    super.visit(ctor);

    String code = "daikon_update();";
    BlockStatement update = (BlockStatement)
      Ast.create("BlockStatement", code);

    // f6 -> ( BlockStatement() )*
    NodeListOptional statements = ctor.f6;
    statements.addNode(update);
    ctor.accept(new TreeFormatter(2, 0));
  }

  // Methods that we have created
  private final Set<MethodDeclaration> generated_methods = new HashSet<MethodDeclaration>();
  /**
   * Renames the original method, and add two new ones that wraps it.
   **/
  public void visit(MethodDeclaration method) {
    super.visit(method);

    if (generated_methods.contains(method)) {
      return;
    }

    // Collect information about how to call the original method.
    String name = Ast.getName(method);
    String returnType = Ast.getReturnType(method);
    String maybeReturn = (returnType.equals("void") ? "" : "return");
    Vector<String> parameters = new Vector<String>();
    for (FormalParameter param : Ast.getParameters(method)) {
      parameters.add(Ast.getName(param));
    }

    StringBuffer pre_body = new StringBuffer();
    pre_body.append("{");
    pre_body.append("  daikon_update();");
    pre_body.append("  " + maybeReturn + " $" + name + "(" + UtilMDE.join(parameters, ", ") + ");");
    pre_body.append("}");

    StringBuffer post_body = new StringBuffer();
    post_body.append("{");
    post_body.append("  try {");
    post_body.append("    " + maybeReturn + " $" + name + "$(" + UtilMDE.join(parameters, ", ") + ");");
    post_body.append("  } finally {");
    post_body.append("    daikon_update();");
    post_body.append("  }");
    post_body.append("}");

    // Create the wrappers as a copies of the original
    String[] new_methods = new String[] {
      pre_body.toString(),
      post_body.toString(),
    };
    for (int i=0; i < new_methods.length; i++) {
      String new_method = new_methods[i];
      MethodDeclaration wrapper = (MethodDeclaration)
        Ast.copy("MethodDeclaration", method);
      Ast.setBody(wrapper, new_method);
      wrapper.accept(new TreeFormatter(2, 0));
      ClassOrInterfaceBody c = (ClassOrInterfaceBody) Ast.getParent(ClassOrInterfaceBody.class, method);
      ClassOrInterfaceBodyDeclaration d = (ClassOrInterfaceBodyDeclaration)
        Ast.create("ClassOrInterfaceBodyDeclaration", Ast.format(wrapper));
      Ast.addDeclaration(c, d);
      MethodDeclaration generated_method = (MethodDeclaration) d.f0.choice;
      if (i == 1) {
        Ast.setName(generated_method, "$" + name);
      }
      generated_methods.add(generated_method);
    }

    // Rename the original implementation, and make it private
    Ast.setName(method, "$" + name + "$");
    Ast.setAccess(method, "private");
  }

  private String getFieldTypeFor(MethodDeclaration method) {
    String type = Ast.getReturnType(method);
    // TODO: handle Iterator, etc.
    return type;
  }

  private String getFieldNameFor(MethodDeclaration method) {
    String name = Ast.getName(method);
    // we renamed the method, but we want the original name
    if (name.charAt(0) == '$') {
      name = name.substring(1);
    }
    // TODO: handle overloading (?), etc.
    return "$method$" + name;
  }

  private String getCallExprFor(MethodDeclaration method) {
    String name = Ast.getName(method);
    if (name.charAt(0) != '$') {
      name = "$" + name + "$";
    }
    return name + "()";
  }

  /**
   * Constructs a list of "@ observer"-annotated methods.
   **/
  public static final class GrepObserversVisitor
    extends DepthFirstVisitor
  {
    public final List<MethodDeclaration> observer_methods = new ArrayList<MethodDeclaration>();

    /**
     * Note all occurences of observer methods.
     **/
    public void visit(NodeToken n) {
      super.visit(n);

      if (n.numSpecials() == 0) { return; }
      for (Enumeration<NodeToken> e = n.specialTokens.elements(); e.hasMoreElements(); ) { // JTB is non-generic
        String comment = e.nextElement().tokenImage;
        if (comment.indexOf("@ observer") >= 0) {
          MethodDeclaration method =
            (MethodDeclaration) Ast.getParent(MethodDeclaration.class, n);
          Assert.assertTrue(method != null);
          String name = Ast.getName(method);
          String returnType = Ast.getReturnType(method);
          if (returnType.equals("void")) {
            System.err.println("Warning: skipping void observer " + name);
            return;
          }
          // System.out.println("Found name: " + name());
          observer_methods.add(method);
        }
      }
    }
  }

  public static void main(String[] args)
    throws IOException
  {
    try {
      mainHelper(args);
    } catch (daikon.Daikon.TerminationMessage e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
    // Any exception other than daikon.Daikon.TerminationMessage gets propagated.
    // This simplifies debugging by showing the stack trace.
  }

  /**
   * This does the work of main, but it never calls System.exit, so it
   * is appropriate to be called progrmmatically.
   * Termination of the program with a message to the user is indicated by
   * throwing daikon.Daikon.TerminationMessage.
   * @see #main(String[])
   * @see daikon.Daikon.TerminationMessage
   **/
  public static void mainHelper(final String[] args)
    throws IOException
  {
    for (int i=0; i < args.length; i++) {
      String javafile = args[i];

      if (! javafile.endsWith(".java")) {
        throw new daikon.Daikon.TerminationMessage("File does not end in .java: " + javafile);
      }

      Node root = null;
      try {
        Reader input = new FileReader(javafile);
        JavaParser parser = new JavaParser(input);
        root = parser.CompilationUnit();
        input.close();
      } catch (ParseException e) {
        e.printStackTrace();
        throw new daikon.Daikon.TerminationMessage("ParseException");
      }

      GrepObserversVisitor grep = new GrepObserversVisitor();
      root.accept(grep);
      root.accept(new TreeFormatter(2, 80));

      InstrumentObserversVisitor instrument =
        new InstrumentObserversVisitor(grep.observer_methods);
      root.accept(instrument);
      root.accept(new TreeFormatter(2, 80));

      Writer output = new FileWriter(new File(javafile));
      root.accept(new TreeDumper(output));
      output.close();
    }
  }

}
