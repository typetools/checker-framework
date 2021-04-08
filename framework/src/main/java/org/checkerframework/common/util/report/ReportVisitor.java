package org.checkerframework.common.util.report;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.util.report.qual.ReportCall;
import org.checkerframework.common.util.report.qual.ReportCreation;
import org.checkerframework.common.util.report.qual.ReportInherit;
import org.checkerframework.common.util.report.qual.ReportOverride;
import org.checkerframework.common.util.report.qual.ReportReadWrite;
import org.checkerframework.common.util.report.qual.ReportUse;
import org.checkerframework.common.util.report.qual.ReportWrite;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

public class ReportVisitor extends BaseTypeVisitor<BaseAnnotatedTypeFactory> {

  /** The tree kinds that should be reported; may be null. */
  private final EnumSet<Tree.Kind> treeKinds;

  /** The modifiers that should be reported; may be null. */
  private final EnumSet<Modifier> modifiers;

  public ReportVisitor(BaseTypeChecker checker) {
    super(checker);

    if (checker.hasOption("reportTreeKinds")) {
      String trees = checker.getOption("reportTreeKinds");
      treeKinds = EnumSet.noneOf(Tree.Kind.class);
      for (String treeKind : trees.split(",")) {
        treeKinds.add(Tree.Kind.valueOf(treeKind.toUpperCase()));
      }
    } else {
      treeKinds = null;
    }

    if (checker.hasOption("reportModifiers")) {
      String mods = checker.getOption("reportModifiers");
      modifiers = EnumSet.noneOf(Modifier.class);
      for (String modifier : mods.split(",")) {
        modifiers.add(Modifier.valueOf(modifier.toUpperCase()));
      }
    } else {
      modifiers = null;
    }
  }

  @SuppressWarnings("compilermessages") // These warnings are not translated.
  @Override
  public Void scan(Tree tree, Void p) {
    if ((tree != null) && (treeKinds != null) && treeKinds.contains(tree.getKind())) {
      checker.reportError(tree, "Tree.Kind." + tree.getKind());
    }
    return super.scan(tree, p);
  }

  /**
   * Check for uses of the {@link ReportUse} annotation. This method has to be called for every
   * explicit or implicit use of a type, most cases are simply covered by the type validator.
   *
   * @param node the tree for error reporting only
   * @param member the element from which to start looking
   */
  private void checkReportUse(Tree node, Element member) {
    Element loop = member;
    while (loop != null) {
      boolean report = this.atypeFactory.getDeclAnnotation(loop, ReportUse.class) != null;
      if (report) {
        checker.reportError(
            node,
            "usage",
            node,
            ElementUtils.getQualifiedName(loop),
            loop.getKind(),
            ElementUtils.getQualifiedName(member),
            member.getKind());
        break;
      } else {
        if (loop.getKind() == ElementKind.PACKAGE) {
          loop = ElementUtils.parentPackage((PackageElement) loop, elements);
          continue;
        }
      }
      // Package will always be the last iteration.
      loop = loop.getEnclosingElement();
    }
  }

  /* Would we want this? Seems redundant, as all uses of the imported
   * package should already be reported.
   * Also, how do we get an element for the import?
  public Void visitImport(ImportTree node, Void p) {
      checkReportUse(node, elem);
  }
  */

  @Override
  public void processClassTree(ClassTree node) {
    TypeElement member = TreeUtils.elementFromDeclaration(node);
    boolean report = false;
    // No need to check on the declaring class itself
    // this.atypeFactory.getDeclAnnotation(member, ReportInherit.class) != null;

    // Check whether any superclass/interface had the ReportInherit annotation.
    List<TypeElement> suptypes = ElementUtils.getSuperTypes(member, elements);
    for (TypeElement sup : suptypes) {
      report = this.atypeFactory.getDeclAnnotation(sup, ReportInherit.class) != null;
      if (report) {
        checker.reportError(node, "inherit", node, ElementUtils.getQualifiedName(sup));
      }
    }
    super.processClassTree(node);
  }

  @Override
  public Void visitMethod(MethodTree node, Void p) {
    ExecutableElement method = TreeUtils.elementFromDeclaration(node);
    boolean report = false;

    // Check all overridden methods.
    Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods =
        AnnotatedTypes.overriddenMethods(elements, atypeFactory, method);
    for (Map.Entry<AnnotatedDeclaredType, ExecutableElement> pair : overriddenMethods.entrySet()) {
      // AnnotatedDeclaredType overriddenType = pair.getKey();
      ExecutableElement exe = pair.getValue();
      report = this.atypeFactory.getDeclAnnotation(exe, ReportOverride.class) != null;
      if (report) {
        // Set method to report the right method, if found.
        method = exe;
        break;
      }
    }

    if (report) {
      checker.reportError(node, "override", node, ElementUtils.getQualifiedName(method));
    }
    return super.visitMethod(node, p);
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
    ExecutableElement method = TreeUtils.elementFromUse(node);
    checkReportUse(node, method);
    boolean report = this.atypeFactory.getDeclAnnotation(method, ReportCall.class) != null;

    if (!report) {
      // Find all methods that are overridden by the called method
      Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods =
          AnnotatedTypes.overriddenMethods(elements, atypeFactory, method);
      for (Map.Entry<AnnotatedDeclaredType, ExecutableElement> pair :
          overriddenMethods.entrySet()) {
        // AnnotatedDeclaredType overriddenType = pair.getKey();
        ExecutableElement exe = pair.getValue();
        report = this.atypeFactory.getDeclAnnotation(exe, ReportCall.class) != null;
        if (report) {
          // Always report the element that has the annotation.
          // Alternative would be to always report the initial element.
          method = exe;
          break;
        }
      }
    }

    if (report) {
      checker.reportError(node, "methodcall", node, ElementUtils.getQualifiedName(method));
    }
    return super.visitMethodInvocation(node, p);
  }

  @Override
  public Void visitMemberSelect(MemberSelectTree node, Void p) {
    Element member = TreeUtils.elementFromUse(node);
    checkReportUse(node, member);
    boolean report = this.atypeFactory.getDeclAnnotation(member, ReportReadWrite.class) != null;

    if (report) {
      checker.reportError(node, "fieldreadwrite", node, ElementUtils.getQualifiedName(member));
    }
    return super.visitMemberSelect(node, p);
  }

  @Override
  public Void visitIdentifier(IdentifierTree node, Void p) {
    Element member = TreeUtils.elementFromUse(node);
    boolean report = this.atypeFactory.getDeclAnnotation(member, ReportReadWrite.class) != null;

    if (report) {
      checker.reportError(node, "fieldreadwrite", node, ElementUtils.getQualifiedName(member));
    }
    return super.visitIdentifier(node, p);
  }

  @Override
  public Void visitAssignment(AssignmentTree node, Void p) {
    Element member = TreeUtils.elementFromUse(node.getVariable());
    boolean report = this.atypeFactory.getDeclAnnotation(member, ReportWrite.class) != null;

    if (report) {
      checker.reportError(node, "fieldwrite", node, ElementUtils.getQualifiedName(member));
    }
    return super.visitAssignment(node, p);
  }

  @Override
  public Void visitArrayAccess(ArrayAccessTree node, Void p) {
    // TODO: should we introduce an annotation for this?
    return super.visitArrayAccess(node, p);
  }

  @Override
  public Void visitNewClass(NewClassTree node, Void p) {
    Element member = TreeUtils.elementFromUse(node);
    boolean report = this.atypeFactory.getDeclAnnotation(member, ReportCreation.class) != null;
    if (!report) {
      // If the constructor is not annotated, check whether the class is.
      member = member.getEnclosingElement();
      report = this.atypeFactory.getDeclAnnotation(member, ReportCreation.class) != null;
    }
    if (!report) {
      // Check whether any superclass/interface had the ReportCreation annotation.
      List<TypeElement> suptypes = ElementUtils.getSuperTypes((TypeElement) member, elements);
      for (TypeElement sup : suptypes) {
        report = this.atypeFactory.getDeclAnnotation(sup, ReportCreation.class) != null;
        if (report) {
          // Set member to report the right member if found
          member = sup;
          break;
        }
      }
    }

    if (report) {
      checker.reportError(node, "creation", node, ElementUtils.getQualifiedName(member));
    }
    return super.visitNewClass(node, p);
  }

  @Override
  public Void visitNewArray(NewArrayTree node, Void p) {
    // TODO Should we report this if the array type is @ReportCreation?
    return super.visitNewArray(node, p);
  }

  @Override
  public Void visitTypeCast(TypeCastTree node, Void p) {
    // TODO Is it worth adding a separate annotation for this?
    return super.visitTypeCast(node, p);
  }

  @Override
  public Void visitInstanceOf(InstanceOfTree node, Void p) {
    // TODO Is it worth adding a separate annotation for this?
    return super.visitInstanceOf(node, p);
  }

  @SuppressWarnings("compilermessages") // These warnings are not translated.
  @Override
  public Void visitModifiers(ModifiersTree node, Void p) {
    if (node != null && modifiers != null) {
      for (Modifier mod : node.getFlags()) {
        if (modifiers.contains(mod)) {
          checker.reportError(node, "Modifier." + mod);
        }
      }
    }
    return super.visitModifiers(node, p);
  }

  @Override
  protected BaseTypeValidator createTypeValidator() {
    return new ReportTypeValidator(checker, this, atypeFactory);
  }

  protected class ReportTypeValidator extends BaseTypeValidator {
    public ReportTypeValidator(
        BaseTypeChecker checker, BaseTypeVisitor<?> visitor, AnnotatedTypeFactory atypeFactory) {
      super(checker, visitor, atypeFactory);
    }

    @Override
    public Void visitDeclared(AnnotatedDeclaredType type, Tree tree) {
      Element member = type.getUnderlyingType().asElement();
      checkReportUse(tree, member);

      return super.visitDeclared(type, tree);
    }
  }
}
