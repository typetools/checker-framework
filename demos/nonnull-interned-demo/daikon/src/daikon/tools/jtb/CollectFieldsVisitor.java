package daikon.tools.jtb;

import java.util.*;
import jtb.syntaxtree.*;
import jtb.visitor.*;
import utilMDE.Assert;

/**
 * Method "fieldDeclarations" returns a list of all FieldDeclarations
 * declared in this class (or, optionally, in nested classes).
 **/
class CollectFieldsVisitor extends DepthFirstVisitor {

  public CollectFieldsVisitor(ClassOrInterfaceDeclaration n, boolean include_nested_classes) {
    this.include_nested_classes = include_nested_classes;
    n.accept(this);
    updateCache();
  }

  /** True if this visitor should include nested classes, false otherwise. **/
  private boolean include_nested_classes;

  private List<FieldDeclaration> fieldDecls = new ArrayList<FieldDeclaration>();

  // Why does this class store arrays instead of, say, List<String>?  Is
  // that just for efficiency?
  private List<String> allNames;
  private List<String> ownedNames;
  private List<String> finalNames;
  // True if the above three lists are up-to-date.
  private boolean cached = false;

  private void updateCache() {
    if (cached) {
      return;
    }
    allNames = new ArrayList<String>();
    ownedNames = new ArrayList<String>();
    finalNames = new ArrayList<String>();
    for (FieldDeclaration fd : fieldDecls) {
      boolean isFinal = hasModifier(fd, "final");
      Type fdtype = fd.f0;
      // See specification in Annotate.java for which fields are owned.
      // boolean isOwned = ! isPrimitive(fdtype);
      boolean isOwned = Ast.isArray(fdtype);
      {
        String name = name(fd.f1);
        allNames.add(name);
        if (isFinal)
          finalNames.add(name);
        if (isOwned)
          ownedNames.add(name);
      }
      NodeListOptional fds = fd.f2;
      if (fds.present()) {
        for (int j=0; j<fds.size(); j++) {
          // System.out.println("" + j + ": " + fds.elementAt(j));
          NodeSequence ns = (NodeSequence) fds.elementAt(j);
          if (ns.size() != 2) {
            System.out.println("Bad length " + ns.size() + " for NodeSequence");
          }
          String name = name((VariableDeclarator) ns.elementAt(1));
          allNames.add(name);
          if (isFinal)
            finalNames.add(name);
          if (isOwned)
            ownedNames.add(name);
        }
      }
    }
    cached = true;
  }

  private String name(VariableDeclarator n) {
    return n.f0.f0.tokenImage;
  }

  private boolean hasModifier(FieldDeclaration n, String mod) {
    return Ast.contains(n.f0, mod);
  }


  /** Returns a list of all FieldDeclarations declared in this class or in
   * nested/inner classes. **/
  public List<FieldDeclaration> fieldDeclarations() {
    updateCache();
    return fieldDecls;
  }

  /** Returns a list of all fields. **/
  public List<String> allFieldNames() {
    updateCache();
    return allNames;
  }

  /** Returns a list of names of all fields with owner annotations. **/
  public List<String> ownedFieldNames() {
    updateCache();
    return ownedNames;
  }

  /** Returns a list of all final fields. **/
  public List<String> finalFieldNames() {
    updateCache();
    return finalNames;
  }

  // Don't continue into nested classes, but do
  // explore them if they are the root.
  private boolean in_class = false;
  public void visit(ClassOrInterfaceDeclaration n) {
    assert ! cached;
    if (include_nested_classes) {
      super.visit(n);             // call "accept(this)" on each field
    } else if (! in_class) {
      // Can't combine these two bodies.  It's wrong to reset in_class to
      // false if it was true (eg, for second level of nested class).
      in_class = true;
      super.visit(n);             // call "accept(this)" on each field
      in_class = false;
    }
  }

  /**
   * f0 -> ( "public" | "protected" | "private" | "static" | "final" | "transient" | "volatile" )*
   * f1 -> Type()
   * f2 -> VariableDeclarator()
   * f3 -> ( "," VariableDeclarator() )*
   * f4 -> ";"
   */
  public void visit(FieldDeclaration n) {
    assert ! cached;
    fieldDecls.add(n);
    super.visit(n);             // call "accept(this)" on each field
  }

}
