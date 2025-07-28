package org.checkerframework.afu.scenelib.el;

import java.util.Map;
import java.util.TreeMap;
import org.checkerframework.afu.scenelib.io.ASTPath;
import org.checkerframework.afu.scenelib.util.coll.VivifyingMap;

/** A declaration, as opposed to an expression. Base class for AClass, AMethod, and AField. */
public abstract class ADeclaration extends AElement {
  /** The element's insert-annotation invocations; map key is the AST path to the insertion place */
  public final VivifyingMap<ASTPath, ATypeElement> insertAnnotations =
      new VivifyingMap<ASTPath, ATypeElement>(new TreeMap<>()) {
        @Override
        public ATypeElement createValueFor(ASTPath k) {
          return new ATypeElement(k);
        }

        @Override
        public boolean isEmptyValue(ATypeElement v) {
          return v.isEmpty();
        }
      };

  /**
   * The element's annotated insert-typecast invocations; map key is the AST path to the insertion
   * place
   */
  public final VivifyingMap<ASTPath, ATypeElementWithType> insertTypecasts =
      new VivifyingMap<ASTPath, ATypeElementWithType>(new TreeMap<>()) {
        @Override
        public ATypeElementWithType createValueFor(ASTPath k) {
          return new ATypeElementWithType(k);
        }

        @Override
        public boolean isEmptyValue(ATypeElementWithType v) {
          return v.isEmpty();
        }
      };

  protected ADeclaration(Object description) {
    super(description, true);
  }

  ADeclaration(ADeclaration decl) {
    super(decl);
    copyMapContents(decl.insertAnnotations, insertAnnotations);
    copyMapContents(decl.insertTypecasts, insertTypecasts);
  }

  ADeclaration(Object description, ADeclaration decl) {
    super(decl, description);
    copyMapContents(decl.insertAnnotations, insertAnnotations);
    copyMapContents(decl.insertTypecasts, insertTypecasts);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof ADeclaration && ((ADeclaration) o).equalsDeclaration(this);
  }

  private boolean equalsDeclaration(ADeclaration o) {
    return super.equals(o)
        && insertAnnotations.equals(o.insertAnnotations)
        && insertTypecasts.equals(o.insertTypecasts);
  }

  @Override
  public int hashCode() {
    return super.hashCode()
        + (insertAnnotations == null ? 0 : insertAnnotations.hashCode())
        + (insertTypecasts == null ? 0 : insertTypecasts.hashCode());
  }

  @Override
  public boolean isEmpty() {
    return super.isEmpty()
        && (insertAnnotations == null || insertAnnotations.isEmpty())
        && (insertTypecasts == null || insertTypecasts.isEmpty());
  }

  @Override
  public void prune() {
    super.prune();
    if (insertAnnotations != null) {
      insertAnnotations.prune();
    }
    if (insertTypecasts != null) {
      insertTypecasts.prune();
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<ASTPath, ATypeElement> em : insertAnnotations.entrySet()) {
      sb.append("insert-annotation: ");
      ASTPath loc = em.getKey();
      sb.append(loc);
      sb.append(": ");
      ATypeElement ae = em.getValue();
      sb.append(ae.toString());
      sb.append(' ');
    }
    for (Map.Entry<ASTPath, ATypeElementWithType> em : insertTypecasts.entrySet()) {
      sb.append("insert-typecast: ");
      ASTPath loc = em.getKey();
      sb.append(loc);
      sb.append(": ");
      AElement ae = em.getValue();
      sb.append(ae.toString());
      sb.append(' ');
    }
    return sb.toString();
  }

  @Override
  public abstract <R, T> R accept(ElementVisitor<R, T> v, T t);
}
