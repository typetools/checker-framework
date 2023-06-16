package org.checkerframework.framework.util.dependenttypes;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Standardizes Java expressions in annotations and also viewpoint-adapts field accesses. Other
 * viewpoint adaption is handled in {@link DependentTypesHelper}.
 */
@AnnotatedFor("nullness")
public class DependentTypesTreeAnnotator extends TreeAnnotator {
  private final DependentTypesHelper helper;

  public DependentTypesTreeAnnotator(
      AnnotatedTypeFactory atypeFactory, DependentTypesHelper helper) {
    super(atypeFactory);
    this.helper = helper;
  }

  @Override
  public Void visitClass(ClassTree tree, AnnotatedTypeMirror annotatedTypeMirror) {
    TypeElement ele = TreeUtils.elementFromDeclaration(tree);
    helper.atTypeDecl(annotatedTypeMirror, ele);
    return super.visitClass(tree, annotatedTypeMirror);
  }

  @Override
  public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror annotatedType) {
    helper.atExpression(annotatedType, tree);
    return super.visitNewArray(tree, annotatedType);
  }

  @Override
  public Void visitTypeCast(TypeCastTree tree, AnnotatedTypeMirror annotatedType) {
    log("DTTA.visitTypeCast(%s, %s)%n", tree, annotatedType);
    helper.atExpression(annotatedType, tree);
    log("DTTA.visitTypeCast(%s, ...) annotatedType=%s; about to call super%n", tree, annotatedType);
    return super.visitTypeCast(tree, annotatedType);
  }

  @Override
  public Void visitVariable(VariableTree tree, AnnotatedTypeMirror annotatedTypeMirror) {
    VariableElement ele = TreeUtils.elementFromDeclaration(tree);
    helper.atVariableDeclaration(annotatedTypeMirror, tree, ele);
    return super.visitVariable(tree, annotatedTypeMirror);
  }

  @Override
  public Void visitIdentifier(IdentifierTree tree, AnnotatedTypeMirror annotatedTypeMirror) {
    Element ele = TreeUtils.elementFromUse(tree);
    if (ele.getKind() == ElementKind.FIELD || ele.getKind() == ElementKind.ENUM_CONSTANT) {
      helper.atVariableDeclaration(annotatedTypeMirror, tree, (VariableElement) ele);
    }
    return super.visitIdentifier(tree, annotatedTypeMirror);
  }

  @Override
  public Void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type) {
    Element ele = TreeUtils.elementFromUse(tree);
    if (ele.getKind() == ElementKind.FIELD || ele.getKind() == ElementKind.ENUM_CONSTANT) {
      helper.atFieldAccess(type, tree);
    }
    return super.visitMemberSelect(tree, type);
  }
}
