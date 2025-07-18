package org.checkerframework.common.basetype;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.ClassGetName;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.javacutil.AbstractTypeProcessor;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.UserError;
import org.plumelib.util.CollectionsPlume;
import org.plumelib.util.StringsPlume;

/**
 * An abstract {@link SourceChecker} that provides a simple {@link
 * org.checkerframework.framework.source.SourceVisitor} implementation that type-checks assignments,
 * pseudo-assignments such as parameter passing and method invocation, and method overriding.
 *
 * <p>Most type-checker annotation processors should extend this class, instead of {@link
 * SourceChecker}. Checkers that require annotated types but not subtype checking (e.g. for testing
 * purposes) should extend {@link SourceChecker}. Non-type checkers (e.g. checkers to enforce coding
 * styles) can extend {@link SourceChecker} or {@link AbstractTypeProcessor}; the Checker Framework
 * is not specifically designed to support such checkers.
 *
 * <p>It is a convention that, for a type system Foo, the checker, the visitor, and the annotated
 * type factory are named as <i>FooChecker</i>, <i>FooVisitor</i>, and
 * <i>FooAnnotatedTypeFactory</i>. Some factory methods use this convention to construct the
 * appropriate classes reflectively.
 *
 * <p>{@code BaseTypeChecker} encapsulates a group for factories for various representations/classes
 * related the type system, mainly:
 *
 * <ul>
 *   <li>{@link QualifierHierarchy}: to represent the supported qualifiers in addition to their
 *       hierarchy, mainly, subtyping rules
 *   <li>{@link TypeHierarchy}: to check subtyping rules between <b>annotated types</b> rather than
 *       qualifiers
 *   <li>{@link AnnotatedTypeFactory}: to construct qualified types enriched with default qualifiers
 *       according to the type system rules
 *   <li>{@link BaseTypeVisitor}: to visit the compiled Java files and check for violations of the
 *       type system rules
 * </ul>
 *
 * <p>Subclasses must specify the set of type qualifiers they support. See {@link
 * AnnotatedTypeFactory#createSupportedTypeQualifiers()}.
 *
 * <p>If the specified type qualifiers are meta-annotated with {@link SubtypeOf}, this
 * implementation will automatically construct the type qualifier hierarchy. Otherwise, or if this
 * behavior must be overridden, the subclass may override the {@link
 * BaseAnnotatedTypeFactory#createQualifierHierarchy()} method.
 *
 * @checker_framework.manual #creating-compiler-interface The checker class
 */
public abstract class BaseTypeChecker extends SourceChecker {

  /** An array containing just {@code BaseTypeChecker.class}. */
  protected static Class<?>[] baseTypeCheckerClassArray = new Class<?>[] {BaseTypeChecker.class};

  /** Create a new BaseTypeChecker. */
  protected BaseTypeChecker() {}

  /**
   * Returns the appropriate visitor that type-checks the compilation unit according to the type
   * system rules.
   *
   * <p>This implementation uses the checker naming convention to create the appropriate visitor. If
   * no visitor is found, it returns an instance of {@link BaseTypeVisitor}. It reflectively invokes
   * the constructor that accepts this checker and the compilation unit tree (in that order) as
   * arguments.
   *
   * <p>Subclasses have to override this method to create the appropriate visitor if they do not
   * follow the checker naming convention.
   *
   * @return the type-checking visitor
   */
  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    // Try to reflectively load the visitor.
    Class<?> checkerClass = this.getClass();
    Object[] thisArray = new Object[] {this};
    while (checkerClass != BaseTypeChecker.class) {
      BaseTypeVisitor<?> result =
          invokeConstructorFor(
              BaseTypeChecker.getRelatedClassName(checkerClass, "Visitor"),
              baseTypeCheckerClassArray,
              thisArray);
      if (result != null) {
        return result;
      }
      checkerClass = checkerClass.getSuperclass();
    }

    // If a visitor couldn't be loaded reflectively, return the default.
    return new BaseTypeVisitor<BaseAnnotatedTypeFactory>(this);
  }

  /**
   * A public variant of {@link #createSourceVisitor}. Only use this if you know what you are doing.
   *
   * @return the type-checking visitor
   */
  public BaseTypeVisitor<?> createSourceVisitorPublic() {
    return createSourceVisitor();
  }

  @Override
  public BaseTypeVisitor<?> getVisitor() {
    return (BaseTypeVisitor<?>) super.getVisitor();
  }

  /**
   * Return the type factory associated with this checker.
   *
   * @return the type factory associated with this checker
   */
  public GenericAnnotatedTypeFactory<?, ?, ?, ?> getTypeFactory() {
    BaseTypeVisitor<?> visitor = getVisitor();
    // Avoid NPE if this method is called during initialization.
    if (visitor == null) {
      throw new TypeSystemError("Called getTypeFactory() before initialization was complete");
    }
    return visitor.getTypeFactory();
  }

  @Override
  public AnnotationProvider getAnnotationProvider() {
    return getTypeFactory();
  }

  /**
   * Returns the type factory used by a subchecker. Returns null if no matching subchecker was found
   * or if the type factory is null. The caller must know the exact checker class to request.
   *
   * <p>Because the visitor state is copied, call this method each time a subfactory is needed
   * rather than store the returned subfactory in a field.
   *
   * @param subCheckerClass the class of the subchecker
   * @param <T> the type of {@code subCheckerClass}'s {@link AnnotatedTypeFactory}
   * @return the type factory of the requested subchecker or null if not found
   */
  @SuppressWarnings("TypeParameterUnusedInFormals") // Intentional abuse
  public <T extends GenericAnnotatedTypeFactory<?, ?, ?, ?>>
      @Nullable T getTypeFactoryOfSubcheckerOrNull(
          Class<? extends BaseTypeChecker> subCheckerClass) {
    return getTypeFactory().getTypeFactoryOfSubcheckerOrNull(subCheckerClass);
  }

  @Override
  protected Object processErrorMessageArg(Object arg) {
    if (arg instanceof Collection) {
      Collection<?> carg = (Collection<?>) arg;
      return CollectionsPlume.mapList(this::processErrorMessageArg, carg);
    } else if (arg instanceof AnnotationMirror && getTypeFactory() != null) {
      return getTypeFactory()
          .getAnnotationFormatter()
          .formatAnnotationMirror((AnnotationMirror) arg);
    } else {
      return super.processErrorMessageArg(arg);
    }
  }

  @Override
  protected boolean shouldAddShutdownHook() {
    if (super.shouldAddShutdownHook() || getTypeFactory().getCFGVisualizer() != null) {
      return true;
    }
    for (SourceChecker checker : getSubcheckers()) {
      if ((checker instanceof BaseTypeChecker)
          && ((BaseTypeChecker) checker).getTypeFactory().getCFGVisualizer() != null) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void shutdownHook() {
    super.shutdownHook();

    CFGVisualizer<?, ?, ?> viz = getTypeFactory().getCFGVisualizer();
    if (viz != null) {
      viz.shutdown();
    }

    for (SourceChecker checker : getSubcheckers()) {
      if (checker instanceof BaseTypeChecker) {
        viz = ((BaseTypeChecker) checker).getTypeFactory().getCFGVisualizer();
        if (viz != null) {
          viz.shutdown();
        }
      }
    }
  }

  @Override
  protected Set<String> createSupportedLintOptions() {
    Set<String> lintSet = super.createSupportedLintOptions();
    lintSet.add("cast");
    lintSet.add("cast:redundant");
    lintSet.add("cast:unsafe");
    return lintSet;
  }

  /** A cache for {@link #getUltimateParentChecker}. */
  protected @MonotonicNonNull BaseTypeChecker ultimateParentChecker;

  /**
   * Finds the ultimate parent checker of this checker. The ultimate parent checker is the checker
   * that the user actually requested, i.e. the one with no parent. The ultimate parent might be
   * this checker itself.
   *
   * @return the first checker in the parent checker chain with no parent checker of its own, i.e.,
   *     the ultimate parent checker
   */
  public BaseTypeChecker getUltimateParentChecker() {
    if (ultimateParentChecker == null) {
      ultimateParentChecker = this;
      while (ultimateParentChecker.getParentChecker() instanceof BaseTypeChecker) {
        ultimateParentChecker = (BaseTypeChecker) ultimateParentChecker.getParentChecker();
      }
    }

    return ultimateParentChecker;
  }

  /**
   * Invokes the constructor belonging to the class named by {@code name} having the given parameter
   * types on the given arguments. Returns {@code null} if the class cannot be found. Otherwise,
   * throws an exception if there is trouble with the constructor invocation.
   *
   * @param <T> the type to which the constructor belongs
   * @param className the name of the class to which the constructor belongs
   * @param paramTypes the types of the constructor's parameters
   * @param args the arguments on which to invoke the constructor
   * @return the result of the constructor invocation on {@code args}, or null if the class does not
   *     exist
   */
  @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"}) // Intentional abuse
  public static <T> @Nullable T invokeConstructorFor(
      @ClassGetName String className, Class<?>[] paramTypes, Object[] args) {

    // Load the class.
    Class<T> cls;
    try {
      cls = (Class<T>) Class.forName(className);
    } catch (Exception e) {
      // no class is found, simply return null
      return null;
    }

    assert cls != null : "reflectively loading " + className + " failed";

    // Invoke the constructor.
    try {
      Constructor<T> ctor = cls.getConstructor(paramTypes);
      return ctor.newInstance(args);
    } catch (Throwable t) {
      if (t instanceof InvocationTargetException) {
        Throwable err = t.getCause();
        if (err instanceof UserError || err instanceof TypeSystemError) {
          // Don't add more information about the constructor invocation.
          throw (RuntimeException) err;
        }
      } else if (t instanceof NoSuchMethodException) {
        // Note: it's possible that NoSuchMethodException was caused by
        // `ctor.newInstance(args)`, if the constructor itself uses reflection.
        // But this case is unlikely.
        throw new TypeSystemError(
            "Could not find constructor %s(%s)", className, StringsPlume.join(", ", paramTypes));
      }

      Throwable cause;
      String causeMessage;
      if (t instanceof InvocationTargetException) {
        cause = t.getCause();
        if (cause == null || cause.getMessage() == null) {
          causeMessage = t.getMessage();
        } else if (t.getMessage() == null) {
          causeMessage = cause.getMessage();
        } else {
          causeMessage = t.getMessage() + ": " + cause.getMessage();
        }
      } else {
        cause = t;
        causeMessage = (cause == null) ? "null" : cause.getMessage();
      }
      throw new BugInCF(
          cause,
          "Error when invoking constructor %s(%s) on args %s; cause: %s",
          className,
          StringsPlume.join(", ", paramTypes),
          Arrays.toString(args),
          causeMessage);
    }
  }
}
