package daikon.tools.runtimechecker;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.List;
import java.util.*;

import jtb.syntaxtree.*;
import jtb.visitor.DepthFirstVisitor;
import jtb.visitor.TreeDumper;
import jtb.visitor.TreeFormatter;
import utilMDE.Assert;
import utilMDE.UtilMDE;
import daikon.PptMap;
import daikon.PptTopLevel;
import daikon.inv.Invariant;
import daikon.inv.OutputFormat;
import daikon.inv.ternary.threeScalar.FunctionBinary;
import daikon.tools.jtb.*;

/**
 * Represents a class created by the instrumenter to check invariants.
 */
public class CheckerClass {

  String name;
  StringBuffer code;
  ClassOrInterfaceBody fclassbody;

  public CheckerClass(ClassOrInterfaceBody clazz) {
    this.fclassbody = clazz;

    // The getClassName method may return some $'s and .'s.
    // Since this is going to become a class name, remove them.
    this.name = Ast.getClassName(clazz).replace("$", "_").replace(".", "_") + "_checks";

    // Get the package and imports from clazz. We'll include them.
    CompilationUnit clazzCU = (CompilationUnit)Ast.getParent(CompilationUnit.class, clazz);
    NodeOptional no = clazzCU.f0;
    String packageName = null;
    if (no.present()) {
      packageName = Ast.format(((PackageDeclaration)no.node).f1).trim();
    } else {
      packageName = "";
    }

    String imports = Ast.format(clazzCU.f1);

    code = new StringBuffer();
    if (!packageName.equals("")) {
      code.append("package " + packageName + ";");
    }
    code.append(imports);
    code.append(" public class " + name + "{ ");
  }

  // See getCompilationUnit().
  private boolean alreadyCalled = false;

  /**
   * Must be called only once, when you're done creating this checker.
   */
  public CompilationUnit getCompilationUnit() {
    if (alreadyCalled) {
      throw new Error("getCompilationUnit should only be called once.");
    }
    alreadyCalled = true;
    code.append("}"); // we're done declaring the class.
    return (CompilationUnit)Ast.create("CompilationUnit", code.toString());
  }

  public String getCheckerClassName() {
    return name;
  }

  public void addDeclaration(StringBuffer decl) {
    code.append(decl);
  }

}
