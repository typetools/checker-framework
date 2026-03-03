package org.checkerframework.framework.type;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
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
import com.sun.source.util.TreePath;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.SwitchExpressionScanner;
import org.checkerframework.javacutil.SwitchExpressionScanner.FunctionalSwitchExpressionScanner;
import org.checkerframework.javacutil.SystemUtil;
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
 * Other expressions are in fact type trees and their annotated type mirrors are computed as type
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

  /** Creates a new TypeFromTreeVisitor. */
  TypeFromExpressionVisitor() {
    // nothing to do
  }

  @Override
  public AnnotatedTypeMirror visitAnnotation(AnnotationTree tree, AnnotatedTypeFactory f) {
    return f.type(tree);
  }

  @Override
  public AnnotatedTypeMirror visitBinary(BinaryTree tree, AnnotatedTypeFactory f) {
    return f.type(tree);
  }

  @Override
  public AnnotatedTypeMirror visitCompoundAssignment(
      CompoundAssignmentTree tree, AnnotatedTypeFactory f) {
    return f.type(tree);
  }

  @Override
  public AnnotatedTypeMirror visitInstanceOf(InstanceOfTree tree, AnnotatedTypeFactory f) {
    return f.type(tree);
  }

  @Override
  public AnnotatedTypeMirror visitLiteral(LiteralTree tree, AnnotatedTypeFactory f) {
    return f.type(tree);
  }

  @Override
  public AnnotatedTypeMirror visitUnary(UnaryTree tree, AnnotatedTypeFactory f) {
    return f.type(tree);
  }

  @Override
  public AnnotatedTypeMirror visitAnnotatedType(AnnotatedTypeTree tree, AnnotatedTypeFactory f) {
    return f.fromTypeTree(tree);
  }

  @Override
  public AnnotatedTypeMirror visitTypeCast(TypeCastTree tree, AnnotatedTypeFactory f) {

    // Use the annotated type of the type in the cast.
    return f.fromTypeTree(tree.getType());
  }

  @Override
  public AnnotatedTypeMirror visitPrimitiveType(PrimitiveTypeTree tree, AnnotatedTypeFactory f) {
    // for e.g. "int.class"
    return f.fromTypeTree(tree);
  }

  @Override
  public AnnotatedTypeMirror visitArrayType(ArrayTypeTree tree, AnnotatedTypeFactory f) {
    // for e.g. "int[].class"
    return f.fromTypeTree(tree);
  }

  @Override
  public AnnotatedTypeMirror visitParameterizedType(
      ParameterizedTypeTree tree, AnnotatedTypeFactory f) {
    return f.fromTypeTree(tree);
  }

  @Override
  public AnnotatedTypeMirror visitIntersectionType(
      IntersectionTypeTree tree, AnnotatedTypeFactory f) {
    return f.fromTypeTree(tree);
  }

  @Override
  public AnnotatedTypeMirror visitMemberReference(
      MemberReferenceTree tree, AnnotatedTypeFactory f) {
    return f.toAnnotatedType(TreeUtils.typeOf(tree), false);
  }

  @Override
  public AnnotatedTypeMirror visitLambdaExpression(
      LambdaExpressionTree tree, AnnotatedTypeFactory f) {
    return f.toAnnotatedType(TreeUtils.typeOf(tree), false);
  }

  @Override
  public AnnotatedTypeMirror visitAssignment(AssignmentTree tree, AnnotatedTypeFactory f) {

    // Recurse on the type of the variable.
    return visit(tree.getVariable(), f);
  }

  @Override
  public AnnotatedTypeMirror visitConditionalExpression(
      ConditionalExpressionTree tree, AnnotatedTypeFactory f) {
    // The Java type of a conditional expression is generally the LUB of the boxed types
    // of the true and false expressions, but with a few exceptions. See JLS 15.25.
    // So, use the type of the ConditionalExpressionTree instead of
    // InternalUtils#leastUpperBound
    TypeMirror alub = TreeUtils.typeOf(tree);

    AnnotatedTypeMirror trueType = f.getAnnotatedType(tree.getTrueExpression());
    AnnotatedTypeMirror falseType = f.getAnnotatedType(tree.getFalseExpression());

    return AnnotatedTypes.leastUpperBound(f, trueType, falseType, alub);
  }

  @Override
  public AnnotatedTypeMirror defaultAction(Tree tree, AnnotatedTypeFactory f) {
    if (SystemUtil.jreVersion >= 14 && tree.getKind().name().equals("SWITCH_EXPRESSION")) {
      return visitSwitchExpressionTree17(tree, f);
    }
    return super.defaultAction(tree, f);
  }

  /**
   * Compute the type of the switch expression tree.
   *
   * @param switchExpressionTree a SwitchExpressionTree; typed as Tree so method signature is
   *     backward-compatible
   * @param f an AnnotatedTypeFactory
   * @return the type of the switch expression
   */
  public AnnotatedTypeMirror visitSwitchExpressionTree17(
      Tree switchExpressionTree, AnnotatedTypeFactory f) {
    TypeMirror switchTypeMirror = TreeUtils.typeOf(switchExpressionTree);
    SwitchExpressionScanner<AnnotatedTypeMirror, Void> luber =
        new FunctionalSwitchExpressionScanner<>(
            // Function applied to each result expression of the switch expression.
            (valueTree, unused) -> f.getAnnotatedType(valueTree),
            // Function used to combine the types of each result expression.
            (type1, type2) -> {
              if (type1 == null) {
                return type2;
              } else if (type2 == null) {
                return type1;
              } else {
                return AnnotatedTypes.leastUpperBound(f, type1, type2, switchTypeMirror);
              }
            });
    return luber.scanSwitchExpression(switchExpressionTree, null);
  }

  @Override
  public AnnotatedTypeMirror visitIdentifier(IdentifierTree tree, AnnotatedTypeFactory f) {
    if (tree.getName().contentEquals("this") || tree.getName().contentEquals("super")) {
      AnnotatedDeclaredType res = f.getSelfType(tree);
      return res;
    }

    Element elt = TreeUtils.elementFromUse(tree);
    AnnotatedTypeMirror selfType = f.getImplicitReceiverType(tree);
    if (selfType != null) {
      AnnotatedTypeMirror type = AnnotatedTypes.asMemberOf(f.types, f, selfType, elt).asUse();
      return f.applyCaptureConversion(type, TreeUtils.typeOf(tree));
    }

    AnnotatedTypeMirror type = f.getAnnotatedType(elt);

    return f.applyCaptureConversion(type, TreeUtils.typeOf(tree));
  }

  @Override
  public AnnotatedTypeMirror visitMemberSelect(MemberSelectTree tree, AnnotatedTypeFactory f) {
    Element elt = TreeUtils.elementFromUse(tree);

    if (TreeUtils.isClassLiteral(tree)) {
      // the type of a class literal is the type of the "class" element.
      return f.getAnnotatedType(elt);
    }
    switch (ElementUtils.getKindRecordAsClass(elt)) {
      case METHOD:
      case CONSTRUCTOR: // x0.super() in anoymous classes
      case PACKAGE: // "java.lang" in new java.lang.Short("2")
      case CLASS: // o instanceof MyClass.InnerClass
      case ENUM:
      case INTERFACE: // o instanceof MyClass.InnerInterface
      case ANNOTATION_TYPE:
        return f.fromElement(elt);
      default:
        // Fall-through.
    }

    if (tree.getIdentifier().contentEquals("this")) {
      // Tree is "MyClass.this", where "MyClass" may be the innermost enclosing type or any
      // outer type.
      return f.getEnclosingType(TypesUtils.getTypeElement(TreeUtils.typeOf(tree)), tree);
    } else if (tree.getIdentifier().contentEquals("super")) {
      // Tree is "MyClass.super", where "MyClass" may be the innermost enclosing type or any
      // outer type.
      TypeMirror superTypeMirror = TreeUtils.typeOf(tree);
      TypeElement superTypeElement = TypesUtils.getTypeElement(superTypeMirror);
      AnnotatedDeclaredType thisType = f.getEnclosingSubType(superTypeElement, tree);
      return AnnotatedTypes.asSuper(
          f, thisType, AnnotatedTypeMirror.createType(superTypeMirror, f, false));
    } else {
      // tree must be a field access or an enum constant, so get the type of the (receiver)
      // expression, and then call asMemberOf.
      AnnotatedTypeMirror typeOfReceiver = f.getAnnotatedType(tree.getExpression());
      typeOfReceiver = f.applyCaptureConversion(typeOfReceiver);
      AnnotatedTypeMirror typeOfFieldAccess =
          AnnotatedTypes.asMemberOf(f.types, f, typeOfReceiver, elt);
      TreePath path = f.getPath(tree);

      // Only capture the type if this is not the left-hand side of an assignment.
      if (path != null && path.getParentPath().getLeaf() instanceof AssignmentTree) {
        AssignmentTree assignmentTree = (AssignmentTree) path.getParentPath().getLeaf();
        @SuppressWarnings("interning:not.interned") // Looking for exact object.
        boolean leftHandSide = assignmentTree.getExpression() != tree;
        if (leftHandSide) {
          return typeOfFieldAccess;
        }
      }
      return f.applyCaptureConversion(typeOfFieldAccess);
    }
  }

  @Override
  public AnnotatedTypeMirror visitArrayAccess(ArrayAccessTree tree, AnnotatedTypeFactory f) {
    AnnotatedTypeMirror type = f.getAnnotatedType(tree.getExpression());
    if (type.getKind() == TypeKind.ARRAY) {
      AnnotatedTypeMirror t = ((AnnotatedArrayType) type).getComponentType();
      t = f.applyCaptureConversion(t);
      return t;
    }
    throw new BugInCF("Unexpected type: " + type);
  }

  @Override
  public AnnotatedTypeMirror visitNewArray(NewArrayTree tree, AnnotatedTypeFactory f) {

    // Don't use fromTypeTree here, because tree.getType() is not an array type!
    AnnotatedArrayType result = (AnnotatedArrayType) f.type(tree);

    if (tree.getType() == null) { // e.g., byte[] b = {(byte)1, (byte)2};
      return result;
    }

    annotateArrayAsArray(result, tree, f);

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

  /**
   * Add annotations to an array type.
   *
   * @param result an array type; is side-effected by this method
   * @param tree an array construction expression from which to obtain annotations
   * @param f the type factory
   */
  private void annotateArrayAsArray(
      AnnotatedArrayType result, NewArrayTree tree, AnnotatedTypeFactory f) {
    // Copy annotations from the type.
    AnnotatedTypeMirror treeElem = f.fromTypeTree(tree.getType());
    boolean hasInit = tree.getInitializers() != null;
    AnnotatedTypeMirror typeElem = descendBy(result, hasInit ? 1 : tree.getDimensions().size());
    while (true) {
      typeElem.addAnnotations(treeElem.getPrimaryAnnotations());
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
      List<? extends AnnotationMirror> annos = TreeUtils.annotationsFromArrayCreation(tree, idx++);
      array.addAnnotations(annos);
      level = array.getComponentType();
    }

    // Add top-level annotations.
    result.addAnnotations(TreeUtils.annotationsFromArrayCreation(tree, -1));
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
   * @param tree a NewClassTree
   * @param f the type factory
   * @return AnnotatedDeclaredType of {@code tree}
   */
  @Override
  public AnnotatedTypeMirror visitNewClass(NewClassTree tree, AnnotatedTypeFactory f) {
    // Add annotations that are on the constructor declaration.
    AnnotatedDeclaredType returnType =
        (AnnotatedDeclaredType) f.constructorFromUse(tree).executableType.getReturnType();
    // Clear the annotations on the return type, so that the explicit annotations can be added
    // first, then the annotations from the return type are added as needed.
    AnnotationMirrorSet fromReturn = new AnnotationMirrorSet(returnType.getPrimaryAnnotations());
    returnType.clearPrimaryAnnotations();
    returnType.addAnnotations(f.getExplicitNewClassAnnos(tree));
    returnType.addMissingAnnotations(fromReturn);
    return returnType;
  }

  @Override
  public AnnotatedTypeMirror visitMethodInvocation(
      MethodInvocationTree tree, AnnotatedTypeFactory f) {
    AnnotatedExecutableType ex = f.methodFromUse(tree).executableType;
    AnnotatedTypeMirror returnT = ex.getReturnType().asUse();
    if (TypesUtils.isCapturedTypeVariable(returnT.getUnderlyingType())
        && !TypesUtils.isCapturedTypeVariable(TreeUtils.typeOf(tree))) {
      // Sometimes javac types an expression as the upper bound of a captured type variable
      // instead of the captured type variable itself. This seems to be a bug in javac. Detect
      // this case and match the annotated type to the Java type.
      returnT = ((AnnotatedTypeVariable) returnT).getUpperBound();
    }

    if (TypesUtils.isRaw(TreeUtils.typeOf(tree))) {
      return returnT.getErased();
    }
    return f.applyCaptureConversion(returnT);
  }

  @Override
  public AnnotatedTypeMirror visitParenthesized(ParenthesizedTree tree, AnnotatedTypeFactory f) {

    // Recurse on the expression inside the parens.
    return visit(tree.getExpression(), f);
  }

  @Override
  public AnnotatedTypeMirror visitWildcard(WildcardTree tree, AnnotatedTypeFactory f) {

    AnnotatedTypeMirror bound = visit(tree.getBound(), f);

    AnnotatedTypeMirror result = f.type(tree);
    assert result instanceof AnnotatedWildcardType;

    // Instead of directly overwriting the bound, replace each annotation
    // to ensure that the structure of the wildcard will match that created by
    // BoundsInitializer/createType.
    if (tree.getKind() == Tree.Kind.SUPER_WILDCARD) {
      f.replaceAnnotations(bound, ((AnnotatedWildcardType) result).getSuperBound());

    } else if (tree.getKind() == Tree.Kind.EXTENDS_WILDCARD) {
      f.replaceAnnotations(bound, ((AnnotatedWildcardType) result).getExtendsBound());
    }
    return result;
  }
}
