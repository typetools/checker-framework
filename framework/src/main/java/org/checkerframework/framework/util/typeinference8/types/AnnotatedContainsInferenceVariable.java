package org.checkerframework.framework.util.typeinference8.types;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNoType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AnnotatedTypeVisitor;

/** Helper class for determining if a type contains an inference variable. */
public class AnnotatedContainsInferenceVariable {

  /** Creates an AnnotatedContainsInferenceVariable. */
  public AnnotatedContainsInferenceVariable() {}

  /**
   * Returns true if {@code type} contains any of the type variables in {@code typeVariables}.
   *
   * @param typeVariables a collection of type variables
   * @param type a type to check
   * @return true if {@code type} contains any of the type variables in {@code typeVariables}
   */
  public static boolean hasAnyTypeVariable(
      Collection<? extends TypeVariable> typeVariables, AnnotatedTypeMirror type) {
    return new Visitor(typeVariables).visit(type);
  }

  /** A helper class to find type variables mentioned by a type. */
  private static class Visitor implements AnnotatedTypeVisitor<Boolean, Void> {

    /** Type variables for which to search. */
    private final Collection<? extends TypeVariable> typeVariables;

    /** A set of types that have been visited. Used to prevent infinite recursion. */
    private final Set<AnnotatedTypeMirror> visitedTypes = new HashSet<>();

    /**
     * Creates the visitor.
     *
     * @param variables a collection of type variables that should be treated as inference variables
     */
    Visitor(Collection<? extends TypeVariable> variables) {
      typeVariables = variables;
    }

    /**
     * Returns true if {@code typeVar} is a type variable in {@code typeVariables}
     *
     * @param typeVar a type variable
     * @return true if {@code typeVar} is a type variable in {@code typeVariables}
     */
    private boolean isTypeVariableOfInterest(AnnotatedTypeVariable typeVar) {
      return typeVariables.contains(typeVar.getUnderlyingType());
    }

    @Override
    public Boolean visit(AnnotatedTypeMirror t, Void aVoid) {
      return t != null && t.accept(this, aVoid);
    }

    @Override
    public Boolean visit(AnnotatedTypeMirror t) {
      return t != null && t.accept(this, null);
    }

    @Override
    public Boolean visitPrimitive(AnnotatedPrimitiveType t, Void aVoid) {
      return false;
    }

    @Override
    public Boolean visitNoType(AnnotatedNoType type, Void unused) {
      return false;
    }

    @Override
    public Boolean visitNull(AnnotatedNullType t, Void aVoid) {
      return false;
    }

    @Override
    public Boolean visitArray(AnnotatedArrayType t, Void aVoid) {
      return visit(t.getComponentType());
    }

    @Override
    public Boolean visitDeclared(AnnotatedDeclaredType t, Void aVoid) {
      boolean found = false;
      for (AnnotatedTypeMirror typeArg : t.getTypeArguments()) {
        if (visit(typeArg)) {
          found = true;
        }
      }
      return found;
    }

    @Override
    public Boolean visitTypeVariable(AnnotatedTypeVariable t, Void aVoid) {
      if (visitedTypes.contains(t)) {
        // t has visited before. If it contained an inference variable,
        // then true would have been returned, so it must not contain an inference variable.
        return false;
      }
      visitedTypes.add(t);
      if (isTypeVariableOfInterest(t)) {
        return true;
      }
      if (visit(t.getLowerBound())) {
        return true;
      }
      return visit(t.getUpperBound());
    }

    @Override
    public Boolean visitWildcard(AnnotatedWildcardType t, Void aVoid) {
      if (visitedTypes.contains(t)) {
        return false;
      }
      visitedTypes.add(t);

      if (visit(t.getSuperBound())) {
        return true;
      }
      return visit(t.getExtendsBound());
    }

    @Override
    public Boolean visitExecutable(AnnotatedExecutableType t, Void aVoid) {
      return false;
    }

    @Override
    public Boolean visitUnion(AnnotatedUnionType t, Void aVoid) {
      for (AnnotatedTypeMirror altern : t.getAlternatives()) {
        if (visit(altern)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public Boolean visitIntersection(AnnotatedIntersectionType t, Void aVoid) {
      for (AnnotatedTypeMirror bound : t.getBounds()) {
        if (visit(bound)) {
          return true;
        }
      }
      return false;
    }
  }
}
