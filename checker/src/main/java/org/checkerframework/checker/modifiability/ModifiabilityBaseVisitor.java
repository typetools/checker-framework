package org.checkerframework.checker.modifiability;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.modifiability.qual.ThrowsUnsupportedOperation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Base visitor for the modifiability sub-checkers (Grow, SeqGrow, Shrink, Replace, and Iterator).
 *
 * <p>This class contains logic shared across all the sub-checkers:
 *
 * <ul>
 *   <li>Suppressing the "constructor result must be TOP" check, since collection constructors may
 *       legitimately produce {@code @Modifiable}.
 *   <li>Suppressing the rule that relates a constructor's result to that of the {@code this()} or
 *       {@code super()} call within it, since a class may declare a different modifiability than
 *       its superclass does.
 *   <li>Requiring all the constructors of a class to declare the same result qualifier, and
 *       requiring the body of each method that requires the capability -- either on its own
 *       receiver parameter or from a method that it overrides -- to agree with that qualifier about
 *       whether the method throws {@link UnsupportedOperationException}.
 *   <li>Requiring a class that claims the capability not to inherit, without overriding, a method
 *       whose implementation always throws {@link UnsupportedOperationException}.
 *   <li>Requiring an override to preserve a positive receiver capability of the method it
 *       overrides.
 * </ul>
 */
public class ModifiabilityBaseVisitor
    extends BaseTypeVisitor<ModifiabilityBaseAnnotatedTypeFactory> {

  /**
   * Create a ModifiabilityBaseVisitor.
   *
   * @param checker the checker that uses this visitor
   */
  public ModifiabilityBaseVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  /**
   * Suppresses the framework's rule that a constructor's result type must be a supertype of the
   * result of the {@code this()} or {@code super()} call within it.
   *
   * <p>A collection class may legitimately declare a different modifiability than its superclass
   * does; for example, a class whose constructors are {@code @Ungrowable} may extend {@code
   * AbstractList}, whose constructor is {@code @Growable}, and override every grow method to throw
   * {@link UnsupportedOperationException}. The framework's rule would reject every such class,
   * including at the implicit {@code super()} call of a constructor that has no explicit one.
   *
   * <p>What makes the suppression less unsafe is that the declared modifiability of a class is
   * checked against its method bodies; see {@link #processClassMembers}. That check is not
   * complete, so the suppression does permit some unsound code: it does nothing unless some
   * constructor of the class declares a qualifier in this hierarchy.
   */
  @Override
  protected void checkThisOrSuperConstructorCall(
      MethodInvocationTree call, @CompilerMessageKey String errorKey) {
    // Do nothing.
  }

  @Override
  public void processClassTree(ClassTree tree) {
    super.processClassTree(tree);
    processClassMembers(tree);
  }

  /**
   * Processes the members of a class: checks that no method writes the bottom qualifier on its
   * receiver, and checks the constructors against one another and against the method bodies.
   *
   * @param tree a class
   */
  private void processClassMembers(ClassTree tree) {
    TypeElement classElement = TreeUtils.elementFromDeclaration(tree);
    if (classElement == null) {
      // Some anonymous classes have no element; see TreeUtils.elementFromDeclaration(ClassTree).
      return;
    }
    TypeMirror classTM = classElement.asType();
    if (!atypeFactory.isRelevant(classTM)) {
      return;
    }

    List<MethodTree> methods = new ArrayList<>();
    List<MethodTree> constructors = new ArrayList<>();
    for (Tree member : tree.getMembers()) {
      if (member instanceof MethodTree mt) {
        if (TreeUtils.isConstructor(mt)) {
          constructors.add(mt);
        } else {
          methods.add(mt);
        }
      }
    }

    // Writing the bottom qualifier on a receiver is an error no matter what the class's
    // constructors say, so this check is done before, and independently of, the checks below.
    for (MethodTree method : methods) {
      checkReceiverNotBottom(method);
    }

    boolean thisClassWarned = false;

    AnnotationMirror constructorAnno = null;
    for (MethodTree constructor : constructors) {
      AnnotatedExecutableType constructorType = atypeFactory.getAnnotatedType(constructor);
      AnnotatedTypeMirror returnType = constructorType.getReturnType();
      AnnotationMirror thisResultAnno =
          returnType.getPrimaryAnnotationInHierarchy(atypeFactory.topAnnotation());
      if (thisResultAnno == null) {
        // The result type has no qualifier in this hierarchy, so there is nothing to compare.
        continue;
      }
      if (constructorAnno == null) {
        constructorAnno = thisResultAnno;
      } else if (!AnnotationUtils.areSameByName(thisResultAnno, constructorAnno)) {
        checker.reportError(
            constructor, "inconsistent.constructor.result.type", thisResultAnno, constructorAnno);
        thisClassWarned = true;
      }
    }
    if (constructorAnno == null) {
      return;
    }

    if (thisClassWarned) {
      return;
    }

    // There is at least one constructor, and all constructors have the same result type
    // annotation.  Examine the implementation of each method that requires the capability.

    for (MethodTree method : methods) {
      if (method.getBody() == null) {
        // The method is abstract or native, so it has no implementation to check.
        continue;
      }
      AnnotationMirror receiverAnno = receiverCapabilityRequirement(method);
      if (receiverAnno != null) {
        checkImplOK(method, receiverAnno, constructorAnno);
      }
    }

    checkInheritedImplementations(tree, classElement, constructorAnno);
  }

  /**
   * Issues an error if the class claims this checker's capability, but inherits without overriding
   * a method whose implementation always throws {@link UnsupportedOperationException}.
   *
   * <p>The body of an inherited method is usually not available -- it is compiled separately, and
   * the checker sees only its signature -- so the implementation is known to throw only if it is
   * annotated {@code @}{@link ThrowsUnsupportedOperation}, as {@code AbstractList.set()} is, or if
   * it is declared in a class whose constructors declare the negative qualifier, in which case
   * {@link #checkImplOK} verified that it throws.
   *
   * @param tree a class
   * @param classElement the element for {@code tree}
   * @param constructorAnno the result qualifier that the class's constructors declare
   */
  private void checkInheritedImplementations(
      ClassTree tree, TypeElement classElement, AnnotationMirror constructorAnno) {
    if (!atypeFactory.hasNegativeCapability()
        || !AnnotationUtils.areSameByName(constructorAnno, positiveCapability())) {
      // The class does not claim the capability, so an inherited implementation that throws
      // UnsupportedOperationException agrees with what the class says about itself.  (In the
      // Iterator hierarchy, which has no negative qualifier, no implementation throws
      // UnsupportedOperationException on account of this checker's capability.)
      return;
    }
    for (ExecutableElement method : ElementFilter.methodsIn(elements.getAllMembers(classElement))) {
      TypeElement declaringClass = ElementUtils.enclosingTypeElement(method);
      if (declaringClass == null || declaringClass.equals(classElement)) {
        // A method that this class declares was checked against its own body, above.
        continue;
      }
      if (!implementationThrowsUOE(method, declaringClass)) {
        continue;
      }
      AnnotatedDeclaredType receiverType = atypeFactory.getAnnotatedType(method).getReceiverType();
      if (receiverType == null || !receiverType.hasPrimaryAnnotation(positiveCapability())) {
        continue;
      }
      checker.reportError(
          tree,
          "inherited.implementation.uoe",
          method,
          declaringClass,
          constructorAnno.getAnnotationType().asElement().getSimpleName());
    }
  }

  /**
   * Returns true if the implementation of {@code method}, which {@code declaringClass} declares,
   * always throws {@link UnsupportedOperationException}.
   *
   * @param method a method that some other class inherits
   * @param declaringClass the class that declares {@code method}
   * @return true if the implementation of {@code method} always throws {@link
   *     UnsupportedOperationException}
   */
  private boolean implementationThrowsUOE(ExecutableElement method, TypeElement declaringClass) {
    if (atypeFactory.getDeclAnnotation(method, ThrowsUnsupportedOperation.class) != null) {
      return true;
    }
    // A class whose constructors declare the negative qualifier, such as @Ungrowable, was itself
    // checked: every method of it that requires the capability throws
    // UnsupportedOperationException.  A class that declares nothing, such as AbstractList, says
    // nothing about its methods, and only the @ThrowsUnsupportedOperation annotation does.
    for (ExecutableElement constructor :
        ElementFilter.constructorsIn(declaringClass.getEnclosedElements())) {
      AnnotationMirror resultAnno =
          atypeFactory
              .getAnnotatedType(constructor)
              .getReturnType()
              .getPrimaryAnnotationInHierarchy(atypeFactory.topAnnotation());
      if (resultAnno != null && isNegativeCapability(resultAnno)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Issues an error if {@code method} writes the bottom qualifier on its receiver parameter.
   *
   * <p>The bottom qualifier is a subtype of every qualifier in the hierarchy, so no value has it
   * and no method can be called on such a receiver. Writing it is always a mistake, whatever the
   * enclosing class's constructors declare, and whether or not the method has a body.
   *
   * @param method a method declaration
   */
  private void checkReceiverNotBottom(MethodTree method) {
    if (method.getReceiverParameter() == null) {
      // The receiver qualifier was defaulted, so the programmer did not write the bottom
      // qualifier on it.
      return;
    }
    AnnotatedDeclaredType receiverType = atypeFactory.getAnnotatedType(method).getReceiverType();
    if (receiverType == null) {
      return;
    }
    AnnotationMirror receiverAnno =
        receiverType.getPrimaryAnnotationInHierarchy(atypeFactory.topAnnotation());
    if (receiverAnno == null) {
      return;
    }
    // Every modifiability hierarchy contains a top qualifier, a positive qualifier, and a
    // polymorphic qualifier.  Every hierarchy but the Iterator one also contains a negative and a
    // bottom qualifier.  There is no predicate for the bottom annotation, so it is recognized by
    // eliminating all the others.  (In the Iterator hierarchy, the positive qualifier is the
    // bottom qualifier, and writing it on a receiver is legitimate.)
    if (AnnotationUtils.areSameByName(receiverAnno, atypeFactory.topAnnotation())
        || AnnotationUtils.areSameByName(receiverAnno, atypeFactory.polyCapability())
        || AnnotationUtils.areSameByName(receiverAnno, positiveCapability())
        || isNegativeCapability(receiverAnno)) {
      return;
    }
    checker.reportError(method, "bottom.annotation.on.receiver");
  }

  /**
   * Returns the qualifier that states what {@code method} requires of its receiver, or null if the
   * method states no requirement.
   *
   * <p>A requirement that the method writes on its own receiver parameter is used directly.
   * Otherwise, the method's receiver qualifier was defaulted -- often from a qualifier on the class
   * declaration, which applies to every method of the class -- so it does not indicate that this
   * method is one of the operations that this checker's capability is about. In that case, the
   * requirement is the one that the method inherits from the methods it overrides, such as the
   * {@code @Growable} receiver of {@code Collection.add()}.
   *
   * @param method a method declaration
   * @return the capability that {@code method} requires of its receiver, or null if none
   */
  private @Nullable AnnotationMirror receiverCapabilityRequirement(MethodTree method) {
    if (method.getReceiverParameter() != null) {
      AnnotatedDeclaredType receiverType = atypeFactory.getAnnotatedType(method).getReceiverType();
      return receiverType == null ? null : receiverType.getAnnotation();
    }

    ExecutableElement methodElt = TreeUtils.elementFromDeclaration(method);
    if (methodElt == null) {
      return null;
    }
    for (Map.Entry<AnnotatedDeclaredType, ExecutableElement> pair :
        AnnotatedTypes.overriddenMethods(elements, atypeFactory, methodElt).entrySet()) {
      AnnotatedExecutableType overriddenMethodType =
          AnnotatedTypes.asMemberOf(types, atypeFactory, pair.getKey(), pair.getValue());
      AnnotatedDeclaredType overriddenReceiver = overriddenMethodType.getReceiverType();
      // Only a positive requirement is inherited.  A defaulted top or polymorphic qualifier says
      // nothing, and a negative or bottom qualifier is not a requirement that this method must
      // live up to.
      if (overriddenReceiver != null
          && overriddenReceiver.hasPrimaryAnnotation(positiveCapability())) {
        return positiveCapability();
      }
    }
    return null;
  }

  /**
   * Issues an error if the method body does not conform to the given annotation.
   *
   * @param method a method declaration
   * @param receiverAnno the annotation on the method's receiver parameter
   * @param constructorAnno the annotation on the class constructors
   */
  private void checkImplOK(
      MethodTree method, AnnotationMirror receiverAnno, AnnotationMirror constructorAnno) {
    if (!AnnotationUtils.areSameByName(receiverAnno, positiveCapability())) {
      // The receiver does not require the capability -- it is the top, polymorphic, negative, or
      // bottom qualifier -- so the method body is unconstrained.  (Writing the bottom qualifier on
      // a receiver is diagnosed by checkReceiverNotBottom.)
      return;
    }

    // `receiverAnno` is positive; that is, the receiver requires the capability.  Whether the
    // method body should throw UnsupportedOperationException depends on the constructor annotation.
    if (!atypeFactory.hasNegativeCapability()) {
      return;
    }
    String constructorAnnoName =
        constructorAnno.getAnnotationType().asElement().getSimpleName().toString();
    if (isNegativeCapability(constructorAnno)) {
      if (!implIsUOE(method, types, elements)) {
        checker.reportError(method, "method.implementation.not.uoe", constructorAnnoName);
      }
    } else if (AnnotationUtils.areSameByName(constructorAnno, positiveCapability())) {
      if (implIsUOE(method, types, elements)) {
        checker.reportError(method, "method.implementation.is.uoe", constructorAnnoName);
      }
    }
    // Otherwise, the constructors' result is the top or the polymorphic qualifier, which claims
    // neither that the class has the capability nor that it lacks it.  Either method body is
    // consistent with such a constructor.
  }

  /**
   * Returns true if {@code anno} is this checker's negative qualifier, such as {@code @Ungrowable}.
   * Always returns false for the Iterator Checker, whose hierarchy has no negative qualifier.
   *
   * @param anno an annotation in this checker's modifiability hierarchy
   * @return true if {@code anno} is this checker's negative qualifier
   */
  private boolean isNegativeCapability(AnnotationMirror anno) {
    return atypeFactory.hasNegativeCapability()
        && AnnotationUtils.areSameByName(anno, atypeFactory.negativeCapability());
  }

  /**
   * Returns true if the method body is exactly {@code throw new
   * UnsupportedOperationException(...)}, where the exception is {@link
   * UnsupportedOperationException} or a subclass of it.
   *
   * @param method a method declaration
   * @param types the type utilities
   * @param elements the element utilities
   * @return true if the method body is exactly {@code throw new UnsupportedOperationException(...)}
   */
  static boolean implIsUOE(MethodTree method, Types types, Elements elements) {
    BlockTree body = method.getBody();
    if (body == null) {
      // The method is abstract or native, so it has no body.
      return false;
    }
    List<? extends StatementTree> statements = body.getStatements();
    if (statements.size() != 1) {
      return false;
    }
    StatementTree statement = statements.get(0);
    if (!(statement instanceof ThrowTree tt)) {
      return false;
    }
    ExpressionTree exception = tt.getExpression();
    if (!(exception instanceof NewClassTree)) {
      return false;
    }
    // Compare the type rather than the name, so that a class that shadows
    // java.lang.UnsupportedOperationException does not satisfy the check, but a subclass of
    // java.lang.UnsupportedOperationException does.
    TypeMirror unsupportedOperationException =
        TypesUtils.typeFromClass(UnsupportedOperationException.class, types, elements);
    return types.isSubtype(
        types.erasure(TreeUtils.typeOf(exception)), types.erasure(unsupportedOperationException));
  }

  /**
   * Returns the positive qualifier for this checker's modifiability hierarchy, such as
   * {@code @Growable}, {@code @SeqGrowable}, {@code @Shrinkable}, or {@code @Replaceable}.
   *
   * @return this checker's positive capability qualifier
   */
  private AnnotationMirror positiveCapability() {
    return atypeFactory.positiveCapability();
  }

  /**
   * Checks the normal override rules, then requires overrides to preserve any positive
   * modifiability receiver capability from the overridden method.
   *
   * <p>For example, if the overridden method requires a {@code @Growable} receiver, then the
   * overriding method must also require a {@code @Growable} receiver.
   *
   * <p>The framework's ordinary receiver override rule allows an overriding method to relax
   * receiver preconditions. For modifiability operations, that would allow a subtype method to drop
   * a required {@code @Growable}, {@code @Shrinkable}, or {@code @Replaceable} receiver capability.
   *
   * <p>For example:
   *
   * <pre>{@code
   * abstract class Super {
   *   abstract void add(@Growable Super this, String s);
   * }
   *
   * class Sub extends Super {
   *   @Override
   *   void add(@MaybeGrowable Sub this, String s) {
   *     throw new UnsupportedOperationException();
   *   }
   * }
   * }</pre>
   *
   * <p>Without requiring the override to preserve the {@code @Growable} receiver, {@code Sub.add()}
   * would be permitted even though the body always throws {@link UnsupportedOperationException}.
   */
  @Override
  protected boolean checkOverride(
      MethodTree overriderTree,
      AnnotatedExecutableType overriderMethodType,
      AnnotatedDeclaredType overriderType,
      AnnotatedExecutableType overriddenMethodType,
      AnnotatedDeclaredType overriddenType) {
    if (!super.checkOverride(
        overriderTree, overriderMethodType, overriderType, overriddenMethodType, overriddenType)) {
      return false;
    }
    // Only capability checkers need to preserve receiver capabilities in overrides.
    // @IteratorPolyMod does not follow this special override rule.
    if (!shouldCheckReceiverOverrideCapabilityPreservation()) {
      return true;
    }

    AnnotatedDeclaredType overriderReceiver = overriderMethodType.getReceiverType();
    AnnotatedDeclaredType overriddenReceiver = overriddenMethodType.getReceiverType();
    if (overriderReceiver == null || overriddenReceiver == null) {
      return true;
    }

    AnnotationMirror positiveCapability = positiveCapability();

    if (overriddenReceiver.hasPrimaryAnnotation(positiveCapability)
        && !overriderReceiver.hasPrimaryAnnotation(positiveCapability)) {
      // Use FoundRequired, as the framework's own `override.receiver` report does, so that the
      // message key is rendered the same way no matter which check produced it.
      FoundRequired pair = FoundRequired.of(overriderReceiver, overriddenReceiver);
      checker.reportError(
          overriderTree,
          "override.receiver",
          pair.found,
          pair.required,
          overriderType,
          overriderMethodType,
          overriddenType,
          overriddenMethodType);
      return false;
    }
    return true;
  }

  /**
   * Returns true if overrides should preserve positive receiver capabilities from overridden
   * methods.
   *
   * @return true if overrides should preserve positive receiver capabilities
   */
  protected boolean shouldCheckReceiverOverrideCapabilityPreservation() {
    return true;
  }

  // Suppresses the framework's "constructor result must be TOP" check.
  // Collection constructors (e.g., new ArrayList()) legitimately produce @Modifiable, which is a
  // subtype of the top type @MaybeModifiable. This suppression is sound: constructors are
  // typed by their stubs or by defaults inferred from the class's annotations, so they cannot
  // silently widen an unmodifiable collection to @Modifiable.
  @Override
  protected void checkConstructorResult(
      AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {}
}
