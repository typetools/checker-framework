package org.checkerframework.framework.util.typeinference8.types;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;

/** Helper class for determining if a type contains an inference variable. */
public class ContainsInferenceVariable {

  /** Returns true if {@code type} contains any of the type variables in {@code typeVariables}. */
  public static boolean hasAnyTypeVariable(
      Collection<? extends TypeVariable> typeVariables, TypeMirror type) {
    return new Visitor(typeVariables).visit(type);
  }

  /** Returns the type variables in {@code typeVariables} that appear in {@code type}. */
  public static LinkedHashSet<TypeVariable> getMentionedTypeVariables(
      Collection<? extends TypeVariable> typeVariables, TypeMirror type) {
    Visitor visitor = new Visitor(typeVariables);
    visitor.visit(type);
    return visitor.foundVariables;
  }

  /** A helper class to find type variables mentioned by a type. */
  static class Visitor implements TypeVisitor<Boolean, Void> {

    /** Type variables for which to search. */
    private final Collection<? extends TypeVariable> typeVariables;

    /** Type variables in {@code typeVariables} that have been found. */
    // default visibility to allow direct access from getMentionedTypeVariables
    final LinkedHashSet<TypeVariable> foundVariables = new LinkedHashSet<>();

    /** A set of types that have been visited. Used to prevent infinite recursion. */
    private final Set<TypeMirror> visitedTypes = new HashSet<>();

    Visitor(Collection<? extends TypeVariable> variables) {
      typeVariables = variables;
    }

    /** Returns true if {@code typeVar} is a type variable in {@code typeVariables} */
    private boolean isTypeVariableOfInterest(TypeVariable typeVar) {
      if (typeVariables.contains(typeVar)) {
        foundVariables.add(typeVar);
        return true;
      }
      return false;
    }

    @Override
    public Boolean visit(TypeMirror t, Void aVoid) {
      return t != null && t.accept(this, aVoid);
    }

    @Override
    public Boolean visit(TypeMirror t) {
      return t != null && t.accept(this, null);
    }

    @Override
    public Boolean visitPrimitive(PrimitiveType t, Void aVoid) {
      return false;
    }

    @Override
    public Boolean visitNull(NullType t, Void aVoid) {
      return false;
    }

    @Override
    public Boolean visitArray(ArrayType t, Void aVoid) {
      return visit(t.getComponentType());
    }

    @Override
    public Boolean visitDeclared(DeclaredType t, Void aVoid) {
      boolean found = false;
      for (TypeMirror typeArg : t.getTypeArguments()) {
        if (visit(typeArg)) {
          found = true;
        }
      }
      return found;
    }

    @Override
    public Boolean visitError(ErrorType t, Void aVoid) {
      return null;
    }

    @Override
    public Boolean visitTypeVariable(TypeVariable t, Void aVoid) {
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
    public Boolean visitWildcard(WildcardType t, Void aVoid) {
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
    public Boolean visitExecutable(ExecutableType t, Void aVoid) {
      return false;
    }

    @Override
    public Boolean visitNoType(NoType t, Void aVoid) {
      return false;
    }

    @Override
    public Boolean visitUnknown(TypeMirror t, Void aVoid) {
      return false;
    }

    @Override
    public Boolean visitUnion(UnionType t, Void aVoid) {
      for (TypeMirror altern : t.getAlternatives()) {
        if (visit(altern)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public Boolean visitIntersection(IntersectionType t, Void aVoid) {
      for (TypeMirror bound : t.getBounds()) {
        if (visit(bound)) {
          return true;
        }
      }
      return false;
    }
  }
}
