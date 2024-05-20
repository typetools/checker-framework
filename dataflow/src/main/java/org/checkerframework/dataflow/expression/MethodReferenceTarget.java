package org.checkerframework.dataflow.expression;

import com.sun.source.tree.MemberReferenceTree;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The part of a <a
 * href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-15.html#jls-15.13">Java Method
 * Reference expression</a> that follows "::". It can have the following forms:
 *
 * <ul>
 *   <li>{@literal [TypeArguments] Identifier}, which may be represented by a standard {@link
 *       javax.lang.model.element.Name}
 *   <li>{@literal [TypeArguments] "new"}, which is a constructor and cannot be represented by an
 *       arbitrary {@link javax.lang.model.element.Name}
 * </ul>
 */
public class MethodReferenceTarget {

  /** The type arguments for this method reference target. */
  private final List<TypeMirror> typeArguments;

  /** The identifier for this method reference target, or null if it is a constructor. */
  private final @Nullable Name identifier;

  /** True if this method reference target is a constructor. */
  private final boolean isConstructor;

  /**
   * Creates a new method reference target.
   *
   * @param typeArguments the type arguments
   * @param identifier the identifier
   * @param isConstructor whether a method reference target is a constructor
   */
  public MethodReferenceTarget(
      List<TypeMirror> typeArguments, @Nullable Name identifier, boolean isConstructor) {
    assert (identifier != null ? 1 : 0) + (isConstructor ? 1 : 0) == 1;
    this.typeArguments = typeArguments;
    this.identifier = identifier;
    this.isConstructor = isConstructor;
  }

  /**
   * Return the type arguments for this method reference target, or null if there are none.
   *
   * @return the type arguments for this method reference target
   */
  @Pure
  public List<TypeMirror> getTypeArguments() {
    return this.typeArguments;
  }

  /**
   * Return the identifier for this method reference target, or null if it's not an identifier (that
   * is, the method reference is for a constructor).
   *
   * @return the identifier for this method reference target
   */
  @Pure
  public @Nullable Name getIdentifier() {
    return this.identifier;
  }

  /**
   * Return true if this method reference target is a constructor (i.e., "new").
   *
   * @return true if this method reference target is a constructor
   */
  public boolean isConstructor() {
    return this.isConstructor;
  }

  /**
   * Return the first subexpression in this method reference target whose class is the given class,
   * or null.
   *
   * @param clazz the class
   * @return the first subexpression in this method reference target whose class is the given class,
   *     or null
   * @param <T> the class
   */
  public <T extends JavaExpression> @Nullable T containedOfClass(Class<T> clazz) {
    T result = null;
    if (!getTypeArguments().isEmpty()) {
      // TODO" look through type arguments and find the class
      result = null;
      if (result != null) {
        return result;
      }
    }
    return result;
  }

  /**
   * Creates a {@link MethodReferenceTarget} given a {@link MemberReferenceTree}
   *
   * <p>A {@link MethodReference} is of the form {@literal
   * MethodReferenceScope::MethodReferenceTarget}, this method constructs the part that follows
   * "::", i.e., the {@link MethodReferenceTarget}.
   *
   * @param tree a member reference tree
   * @return a method reference target
   */
  public static MethodReferenceTarget fromMemberReferenceTree(MemberReferenceTree tree) {
    List<TypeMirror> typeArguments = Collections.emptyList();
    if (tree.getTypeArguments() != null) {
      typeArguments =
          tree.getTypeArguments().stream().map(TreeUtils::typeOf).collect(Collectors.toList());
    }
    Name methodName = tree.getName();
    return new MethodReferenceTarget(
        typeArguments, methodName, methodName.toString().equals("new"));
  }

  @Override
  public String toString() {
    @SuppressWarnings("nullness:dereference.of.nullable") // !isConstructor() => identifier != null
    String targetName = isConstructor() ? "new" : identifier.toString();
    if (typeArguments.isEmpty()) {
      return targetName;
    }
    StringJoiner typeArgString = new StringJoiner(", ", "<", ">");
    for (TypeMirror typeArg : typeArguments) {
      typeArgString.add(typeArg.toString());
    }
    return typeArgString.toString() + targetName;
  }
}
