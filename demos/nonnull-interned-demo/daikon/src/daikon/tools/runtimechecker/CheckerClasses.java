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
 * Represents a set of classes created by the instrumenter to check
 * invariants.
 */
public class CheckerClasses {

  protected List<CheckerClass> classes = new ArrayList<CheckerClass>();

  public void addCheckerClass(ClassOrInterfaceBody clazz) {
    classes.add(new CheckerClass(clazz));
  }

  public List<CheckerClass> getCheckerClasses() {
    return classes;
  }

  public void addDeclaration(ClassOrInterfaceBody clazz,
                             StringBuffer decl) {

    for (CheckerClass c : classes) {
      if (c.fclassbody == clazz) {
        c.addDeclaration(decl);
      }
    }

  }

  public void addDeclaration(ConstructorDeclaration clazz,
                             StringBuffer decl) {

    addDeclaration((ClassOrInterfaceBody)Ast.getParent(ClassOrInterfaceBody.class, clazz), decl);
  }

  public void addDeclaration(MethodDeclaration clazz,
                             StringBuffer decl) {

    addDeclaration((ClassOrInterfaceBody)Ast.getParent(ClassOrInterfaceBody.class, clazz), decl);
  }


}
