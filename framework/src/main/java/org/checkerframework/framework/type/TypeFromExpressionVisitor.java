package org.checkerframework.framework.type;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.WildcardTree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Converts ExpressionTrees into AnnotatedTypeMirrors.
 *
 * <p>The type of some expressions depends on the checker, so for these expressions, a checker
 * should add annotations in a {@link
 * org.checkerframework.framework.type.treeannotator.TreeAnnotator} and/or the {@link
 * org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator}. These trees are:
 *
 * <ul>
 *   <li>{@code BinaryTree}
 *   <li>{@code CompoundAssignmentTree}
 *   <li>{@code InstanceOfTree}
 *   <li>{@code LiteralTree}
 *   <li>{@code UnaryTree}
 * </ul>
 *
 * Other expressions are in fact type trees and their annotataed type mirrors are computed as type
 * trees:
 *
 * <ul>
 *   <li>{@code AnnotatedTypeTree}
 *   <li>{@code TypeCastTree}
 *   <li>{@code PrimitiveTypeTree}
 *   <li>{@code ArrayTypeTree}
 *   <li>{@code ParameterizedTypeTree}
 *   <li>{@code IntersectionTypeTree}
 * </ul>
 */
class TypeFromExpressionVisitor extends TypeFromTreeVisitor {

  @Override
  public AnnotatedTypeMirror visitBinary(BinaryTree node, AnnotatedTypeFactory f) {
    return f.type(node);
  }

  @Override
  public AnnotatedTypeMirror visitCompoundAssignment(
      CompoundAssignmentTree node, AnnotatedTypeFactory f) {
    return f.type(node);
  }

  @Override
  public AnnotatedTypeMirror visitInstanceOf(InstanceOfTree node, AnnotatedTypeFactory f) {
    return f.type(node);
  }

  @Override
  public AnnotatedTypeMirror visitLiteral(LiteralTree node, AnnotatedTypeFactory f) {
    return f.type(node);
  }

  @Override
  public AnnotatedTypeMirror visitUnary(UnaryTree node, AnnotatedTypeFactory f) {
    return f.type(node);
  }

  @Override
  public AnnotatedTypeMirror visitAnnotatedType(AnnotatedTypeTree node, AnnotatedTypeFactory f) {
    return f.fromTypeTree(node);
  }

  @Override
  public AnnotatedTypeMirror visitTypeCast(TypeCastTree node, AnnotatedTypeFactory f) {

    // Use the annotated type of the type in the cast.
    return f.fromTypeTree(node.getType());
  }

  @Override
  public AnnotatedTypeMirror visitPrimitiveType(PrimitiveTypeTree node, AnnotatedTypeFactory f) {
    // for e.g. "int.class"
    return f.fromTypeTree(node);
  }

  @Override
  public AnnotatedTypeMirror visitArrayType(ArrayTypeTree node, AnnotatedTypeFactory f) {
    // for e.g. "int[].class"
    return f.fromTypeTree(node);
  }

  @Override
  public AnnotatedTypeMirror visitParameterizedType(
      ParameterizedTypeTree node, AnnotatedTypeFactory f) {
    return f.fromTypeTree(node);
  }

  @Override
  public AnnotatedTypeMirror visitIntersectionType(
      IntersectionTypeTree node, AnnotatedTypeFactory f) {
    return f.fromTypeTree(node);
  }

  @Override
  public AnnotatedTypeMirror visitMemberReference(
      MemberReferenceTree node, AnnotatedTypeFactory f) {
    return f.toAnnotatedType(TreeUtils.typeOf(node), false);
  }

  @Override
  public AnnotatedTypeMirror visitLambdaExpression(
      LambdaExpressionTree node, AnnotatedTypeFactory f) {
    return f.toAnnotatedType(TreeUtils.typeOf(node), false);
  }

  @Override
  public AnnotatedTypeMirror visitAssignment(AssignmentTree node, AnnotatedTypeFactory f) {

    // Recurse on the type of the variable.
    return visit(node.getVariable(), f);
  }

  @Override
  public AnnotatedTypeMirror visitConditionalExpression(
      ConditionalExpressionTree node, AnnotatedTypeFactory f) {
    // The Java type of a conditional expression is generally the LUB of the boxed types
    // of the true and false expressions, but with a few exceptions. See JLS 15.25.
    // So, use the type of the ConditionalExpressionTree instead of
    // InternalUtils#leastUpperBound
    TypeMirror alub = TreeUtils.typeOf(node);

    AnnotatedTypeMirror trueType = f.getAnnotatedType(node.getTrueExpression());
    AnnotatedTypeMirror falseType = f.getAnnotatedType(node.getFalseExpression());

    return AnnotatedTypes.leastUpperBound(f, trueType, falseType, alub);
  }

  @Override
  public AnnotatedTypeMirror visitIdentifier(IdentifierTree node, AnnotatedTypeFactory f) {
    if (node.getName().contentEquals("this") || node.getName().contentEquals("super")) {
      AnnotatedDeclaredType res = f.getSelfType(node);
      return res;
    }

    Element elt = TreeUtils.elementFromUse(node);
    AnnotatedTypeMirror selfType = f.getImplicitReceiverType(node);
    if (selfType != null) {
      return AnnotatedTypes.asMemberOf(f.types, f, selfType, elt).asUse();
    }

    AnnotatedTypeMirror type = f.getAnnotatedType(elt);

    return f.applyCaptureConversion(type, TreeUtils.typeOf(node));
  }

  @Override
  public AnnotatedTypeMirror visitMemberSelect(MemberSelectTree node, AnnotatedTypeFactory f) {
    Element elt = TreeUtils.elementFromUse(node);

    if (TreeUtils.isClassLiteral(node)) {
      // the type of a class literal is the type of the "class" element.
      return f.getAnnotatedType(elt);
    }
    switch (elt.getKind()) {
      case METHOD:
      case PACKAGE: // "java.lang" in new java.lang.Short("2")
      case CLASS: // o instanceof MyClass.InnerClass
      case ENUM:
      case INTERFACE: // o instanceof MyClass.InnerInterface
      case ANNOTATION_TYPE:
        return f.fromElement(elt);
      default:
        // Fall-through.
    }

    if (node.getIdentifier().contentEquals("this")) {
      // Node is "MyClass.this", where "MyClass" may be the innermost enclosing type or any
      // outer type.
      return f.getEnclosingType(TypesUtils.getTypeElement(TreeUtils.typeOf(node)), node);
    } else {
      // node must be a field access, so get the type of the expression, and then call asMemberOf.
      AnnotatedTypeMirror t = f.getAnnotatedType(node.getExpression());
      t = f.applyCaptureConversion(t);
      return AnnotatedTypes.asMemberOf(f.types, f, t, elt).asUse();
    }
  }

  @Override
  public AnnotatedTypeMirror visitArrayAccess(ArrayAccessTree node, AnnotatedTypeFactory f) {

    Pair<Tree, AnnotatedTypeMirror> preAssignmentContext = f.visitorState.getAssignmentContext();
    try {
      // TODO: what other trees shouldn't maintain the context?
      f.visitorState.setAssignmentContext(null);

      AnnotatedTypeMirror type = f.getAnnotatedType(node.getExpression());
      if (type.getKind() == TypeKind.ARRAY) {
        return ((AnnotatedArrayType) type).getComponentType();
      } else if (type.getKind() == TypeKind.WILDCARD
          && ((AnnotatedWildcardType) type).isUninferredTypeArgument()) {
        // Clean-up after Issue #979.
        AnnotatedTypeMirror wcbound = ((AnnotatedWildcardType) type).getExtendsBound();
        if (wcbound instanceof AnnotatedArrayType) {
          return ((AnnotatedArrayType) wcbound).getComponentType();
        }
      }
      throw new BugInCF("Unexpected type: " + type);
    } finally {
      f.visitorState.setAssignmentContext(preAssignmentContext);
    }
  }

  @Override
  public AnnotatedTypeMirror visitNewArray(NewArrayTree node, AnnotatedTypeFactory f) {

    // Don't use fromTypeTree here, because node.getType() is not an array type!
    AnnotatedArrayType result = (AnnotatedArrayType) f.type(node);

    if (node.getType() == null) { // e.g., byte[] b = {(byte)1, (byte)2};
      return result;
    }

    annotateArrayAsArray(result, node, f);

    return result;
  }

  private AnnotatedTypeMirror descendBy(AnnotatedTypeMirror type, int depth) {
    AnnotatedTypeMirror result = type;
    while (depth > 0) {
      result = ((AnnotatedArrayType) result).getComponentType();
      depth--;
    }
    return result;
  }

  private void annotateArrayAsArray(
      AnnotatedArrayType result, NewArrayTree node, AnnotatedTypeFactory f) {
    // Copy annotations from the type.
    AnnotatedTypeMirror treeElem = f.fromTypeTree(node.getType());
    boolean hasInit = node.getInitializers() != null;
    AnnotatedTypeMirror typeElem = descendBy(result, hasInit ? 1 : node.getDimensions().size());
    while (true) {
      typeElem.addAnnotations(treeElem.getAnnotations());
      if (!(treeElem instanceof AnnotatedArrayType)) {
        break;
      }
      assert typeElem instanceof AnnotatedArrayType;
      treeElem = ((AnnotatedArrayType) treeElem).getComponentType();
      typeElem = ((AnnotatedArrayType) typeElem).getComponentType();
    }
    // Add all dimension annotations.
    int idx = 0;
    AnnotatedTypeMirror level = result;
    while (level.getKind() == TypeKind.ARRAY) {
      AnnotatedArrayType array = (AnnotatedArrayType) level;
      List<? extends AnnotationMirror> annos = TreeUtils.annotationsFromArrayCreation(node, idx++);
      array.addAnnotations(annos);
      level = array.getComponentType();
    }

    // Add top-level annotations.
    result.addAnnotations(TreeUtils.annotationsFromArrayCreation(node, -1));
  }

  /**
   * Creates an AnnotatedDeclaredType for the NewClassTree and adds, for each hierarchy, one of:
   *
   * <ul>
   *   <li>an explicit annotation on the new class expression ({@code new @HERE MyClass()}), or
   *   <li>an explicit annotation on the declaration of the class ({@code @HERE class MyClass {}}),
   *       or
   *   <li>an explicit or default annotation on the declaration of the constructor ({@code @HERE
   *       public MyClass() {}}).
   * </ul>
   *
   * @param node NewClassTree
   * @param f the type factory
   * @return AnnotatedDeclaredType of {@code node}
   */
  @Override
  public AnnotatedTypeMirror visitNewClass(NewClassTree node, AnnotatedTypeFactory f) {
    // constructorFromUse return type has default annotations
    // so use fromNewClass which does diamond inference and only
    // contains explicit annotations.
    AnnotatedDeclaredType type = f.fromNewClass(node);

    // Add annotations that are on the constructor declaration.
    AnnotatedExecutableType ex = f.constructorFromUse(node).executableType;
    type.addMissingAnnotations(ex.getReturnType().getAnnotations());

    return type;
  }

  @Override
  public AnnotatedTypeMirror visitMethodInvocation(
      MethodInvocationTree node, AnnotatedTypeFactory f) {
    AnnotatedExecutableType ex = f.methodFromUse(node).executableType;
    return ex.getReturnType().asUse();
  }

  @Override
  public AnnotatedTypeMirror visitParenthesized(ParenthesizedTree node, AnnotatedTypeFactory f) {

    // Recurse on the expression inside the parens.
    return visit(node.getExpression(), f);
  }

  @Override
  public AnnotatedTypeMirror visitWildcard(WildcardTree node, AnnotatedTypeFactory f) {

    AnnotatedTypeMirror bound = visit(node.getBound(), f);

    AnnotatedTypeMirror result = f.type(node);
    assert result instanceof AnnotatedWildcardType;

    // Instead of directly overwriting the bound, replace each annotation
    // to ensure that the structure of the wildcard will match that created by
    // BoundsInitializer/createType.
    if (node.getKind() == Tree.Kind.SUPER_WILDCARD) {
      f.replaceAnnotations(bound, ((AnnotatedWildcardType) result).getSuperBound());

    } else if (node.getKind() == Tree.Kind.EXTENDS_WILDCARD) {
      f.replaceAnnotations(bound, ((AnnotatedWildcardType) result).getExtendsBound());
    }
    return result;
  }
}
