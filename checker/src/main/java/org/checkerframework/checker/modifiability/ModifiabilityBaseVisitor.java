package org.checkerframework.checker.modifiability;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
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
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

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
   * checked against its method bodies; see {@link #processClassConstructors}. That check is not
   * complete, so the suppression does permit some unsound code. The check does nothing unless some
   * constructor of the class declares a qualifier in this hierarchy, and it examines only the
   * methods that the class declares, not those that it inherits without overriding.
   */
  @Override
  protected void checkThisOrSuperConstructorCall(
      MethodInvocationTree call, @CompilerMessageKey String errorKey) {
    // Do nothing.
  }

  @Override
  public void processClassTree(ClassTree tree) {
    super.processClassTree(tree);
    processClassConstructors(tree);
  }

  /**
   * Processes the constructors of a class.
   *
   * @param tree a class
   */
  private void processClassConstructors(ClassTree tree) {
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
    // Every modifiability hierarchy contains a top qualifier, a positive qualifier, and a
    // polymorphic qualifier.  Every hierarchy but the Iterator one also contains a negative and a
    // bottom qualifier.
    if (AnnotationUtils.areSameByName(receiverAnno, atypeFactory.topAnnotation())
        || AnnotationUtils.areSameByName(receiverAnno, atypeFactory.polyCapability())
        || isNegativeCapability(receiverAnno)) {
      // Nothing to check.
      return;
    }
    // There is no predicate for the bottom annotation.
    if (!AnnotationUtils.areSameByName(receiverAnno, positiveCapability())) {
      // The only qualifier left is the bottom one.
      checker.reportError(method, "bottom.annotation.on.receiver");
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
      if (!implIsUOE(method)) {
        checker.reportError(method, "method.implementation.not.uoe", constructorAnnoName);
      }
    } else if (AnnotationUtils.areSameByName(constructorAnno, positiveCapability())) {
      if (implIsUOE(method)) {
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
   * UnsupportedOperationException(...)}.
   *
   * @param method a method declaration
   * @return true if the method body is exactly {@code throw new UnsupportedOperationException(...)}
   */
  private boolean implIsUOE(MethodTree method) {
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
    if (!(exception instanceof NewClassTree nct)) {
      return false;
    }
    ExpressionTree identifier = nct.getIdentifier();
    if (identifier instanceof IdentifierTree it) {
      // TODO: This can be fooled if a different UnsupportedOperationException is imported.
      // A way to prevent that, is to check the type of exception:
      // types.isSameType(TreeUtils.typeOf(exception), ...);
      return it.getName().contentEquals("UnsupportedOperationException");
    } else if (identifier instanceof MemberSelectTree mst) {
      // TODO: For efficiency, to avoid call to `toString()`, could walk down the MemberSelectTree.
      return mst.toString().equals("java.lang.UnsupportedOperationException");
    }
    return false;
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
      checker.reportError(
          overriderTree,
          "override.receiver",
          overriderReceiver,
          overriddenReceiver,
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
