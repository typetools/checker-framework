package org.checkerframework.framework.ajava;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.framework.stub.AnnotationFileParser;

/**
 * Moves annotations in a JavaParser AST from declaration position onto the types they correspond
 * to.
 *
 * <p>When parsing a method or field such as {@code @Tainted String myField}, JavaParser puts all
 * annotations on the declaration.
 *
 * <p>For each non-declaration annotation on a method or field declaration, this class moves it to
 * the type position. A non-declaration annotation is one with a {@code TYPE_USE} target but no
 * declaration target.
 */
public class TypeAnnotationMover extends VoidVisitorAdapter<Void> {
  /**
   * Annotations imported by the file, stored as a mapping from names to the TypeElements for the
   * annotations. Contains entries for the simple and fully qualified names of each annotation.
   */
  private Map<String, TypeElement> allAnnotations;
  /** Element utilities. */
  private Elements elements;

  /**
   * Constructs a {@code TypeAnnotationMover}.
   *
   * @param allAnnotations the annotations imported by the file, as a mapping from annotation name
   *     to TypeElement. There should be two entries for each annotation: the annotation's simple
   *     name and its fully-qualified name both mapped to its TypeElement.
   * @param elements Element utilities
   */
  public TypeAnnotationMover(Map<String, TypeElement> allAnnotations, Elements elements) {
    this.allAnnotations = new HashMap<>(allAnnotations);
    this.elements = elements;
  }

  @Override
  public void visit(FieldDeclaration node, Void p) {
    // Use the type of the first declared variable in the field declaration.
    Type type = node.getVariable(0).getType();
    if (!type.isClassOrInterfaceType()) {
      return;
    }

    if (isMultiPartName(type)) {
      return;
    }

    List<AnnotationExpr> annosToMove = getAnnotationsToMove(node, ElementType.FIELD);
    if (annosToMove.isEmpty()) {
      return;
    }

    node.getAnnotations().removeAll(annosToMove);
    annosToMove.forEach(anno -> type.asClassOrInterfaceType().addAnnotation(anno));
  }

  @Override
  public void visit(MethodDeclaration node, Void p) {
    Type type = node.getType();
    if (!type.isClassOrInterfaceType()) {
      return;
    }

    if (isMultiPartName(type)) {
      return;
    }

    List<AnnotationExpr> annosToMove = getAnnotationsToMove(node, ElementType.METHOD);
    if (annosToMove.isEmpty()) {
      return;
    }

    node.getAnnotations().removeAll(annosToMove);
    annosToMove.forEach(anno -> type.asClassOrInterfaceType().addAnnotation(anno));
  }

  /**
   * Given a declaration, returns a List of annotations currently in declaration position that can't
   * possibly be declaration annotations for that type of declaration.
   *
   * @param node JavaParser node for declaration
   * @param declarationType the type of declaration {@code node} represents; always FIELD or METHOD
   * @return a list of annotations in declaration position that should be on the declaration's type
   */
  private List<AnnotationExpr> getAnnotationsToMove(
      NodeWithAnnotations<?> node, ElementType declarationType) {
    List<AnnotationExpr> annosToMove = new ArrayList<>();
    for (AnnotationExpr annotation : node.getAnnotations()) {
      if (!isPossiblyDeclarationAnnotation(annotation, declarationType)) {
        annosToMove.add(annotation);
      }
    }

    return annosToMove;
  }

  /**
   * Returns the TypeElement for an annotation, or null if it cannot be found.
   *
   * @param annotation a JavaParser annotation
   * @return the TypeElement for {@code annotation}, or null if it cannot be found
   */
  private @Nullable TypeElement getAnnotationDeclaration(AnnotationExpr annotation) {
    @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
    @FullyQualifiedName String annoNameFq = annotation.getNameAsString();
    TypeElement annoTypeElt = allAnnotations.get(annoNameFq);
    if (annoTypeElt == null) {
      annoTypeElt = elements.getTypeElement(annoNameFq);
      if (annoTypeElt == null) {
        // Not a supported annotation.
        return null;
      }
      AnnotationFileParser.putAllNew(
          allAnnotations,
          AnnotationFileParser.createNameToAnnotationMap(Collections.singletonList(annoTypeElt)));
    }

    return annoTypeElt;
  }

  /**
   * Returns if {@code annotation} could be a declaration annotation for {@code declarationType}.
   * This would be the case if the annotation isn't recognized at all, or if it has no
   * {@code @Target} meta-annotation, or if it has {@code declarationType} as one of its targets.
   *
   * @param annotation a JavaParser annotation expression
   * @param declarationType the declaration type to check if {@code annotation} might be a
   *     declaration annotation for
   * @return true unless {@code annotation} definitely cannot be a declaration annotation for {@code
   *     declarationType}
   */
  private boolean isPossiblyDeclarationAnnotation(
      AnnotationExpr annotation, ElementType declarationType) {
    TypeElement annotationType = getAnnotationDeclaration(annotation);
    if (annotationType == null) {
      return true;
    }

    return isDeclarationAnnotation(annotationType, declarationType);
  }

  /**
   * Returns whether the annotation represented by {@code annotationDeclaration} might be a
   * declaration annotation for {@code declarationType}. This holds if the TypeElement has no
   * {@code @Target} meta-annotation, or if {@code declarationType} is a target of the annotation.
   *
   * @param annotationDeclaration declaration for an annotation
   * @param declarationType the declaration type to check if the annotation might be a declaration
   *     annotation for
   * @return true if {@code annotationDeclaration} contains {@code declarationType} as a target or
   *     doesn't contain {@code ElementType.TYPE_USE} as a target
   */
  private boolean isDeclarationAnnotation(
      TypeElement annotationDeclaration, ElementType declarationType) {
    Target target = annotationDeclaration.getAnnotation(Target.class);
    if (target == null) {
      return true;
    }

    boolean hasTypeUse = false;
    for (ElementType elementType : target.value()) {
      if (elementType == declarationType) {
        return true;
      }

      if (elementType == ElementType.TYPE_USE) {
        hasTypeUse = true;
      }
    }

    if (!hasTypeUse) {
      throw new BugInCF(
          "Annotation %s cannot be used on declaration with type %s",
          annotationDeclaration.getQualifiedName(), declarationType);
    }
    return false;
  }

  /**
   * Returns whether {@code type} has a name containing multiple parts separated by dots, e.g.
   * "java.lang.String" or "Outer.Inner".
   *
   * <p>Annotations should not be moved onto a Type for which this method returns true. A type like
   * {@code @Anno java.lang.String} is illegal since the annotation should go directly next to the
   * rightmost part of the fully qualified name, like {@code java.lang. @Anno String}. So if a file
   * contains a declaration like {@code @Anno java.lang.String myField}, the annotation must belong
   * to the declaration and not the type.
   *
   * <p>If a declaration contains an inner class type like {@code @Anno Outer.Inner myField}, it may
   * be the case that {@code @Anno} belongs to the type {@code Outer}, not the declaration, and
   * should be moved, but it's impossible to distinguish this from the above case using only the
   * JavaParser AST for a file. To be safe, the annotation still shouldn't be moved, but this may
   * lead to suboptimal formatting placing {@code @Anno} on its own line.
   *
   * @param type a JavaParser type node
   * @return true if {@code type} has a multi-part name
   */
  private boolean isMultiPartName(Type type) {
    return type.isClassOrInterfaceType() && type.asClassOrInterfaceType().getScope().isPresent();
  }
}
