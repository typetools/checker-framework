package org.checkerframework.afu.scenelib.el;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import org.checkerframework.afu.scenelib.util.coll.VivifyingMap;

/**
 * Manages all annotations within expressions, that is, annotations on typecasts, instanceofs, and
 * object creations. We can use this class for methods, field initializers, and static initializers.
 */
public class AExpression extends AElement {
  /** The method's annotated typecasts; map key is the offset of the checkcast bytecode */
  public final VivifyingMap<RelativeLocation, ATypeElement> typecasts =
      ATypeElement.<RelativeLocation>newVivifyingLHMap_ATE();

  /** The method's annotated "instanceof" tests; map key is the offset of the instanceof bytecode */
  public final VivifyingMap<RelativeLocation, ATypeElement> instanceofs =
      ATypeElement.<RelativeLocation>newVivifyingLHMap_ATE();

  /** The method's annotated "new" invocations; map key is the offset of the new bytecode */
  public final VivifyingMap<RelativeLocation, ATypeElement> news =
      ATypeElement.<RelativeLocation>newVivifyingLHMap_ATE();

  /**
   * A method invocation's annotated type arguments; map key is the offset of the invokestatic
   * bytecode
   */
  public final VivifyingMap<RelativeLocation, ATypeElement> calls =
      ATypeElement.<RelativeLocation>newVivifyingLHMap_ATE();

  /**
   * A member reference's annotated type parameters; map key is the offset of the invokestatic
   * bytecode
   */
  public final VivifyingMap<RelativeLocation, ATypeElement> refs =
      ATypeElement.<RelativeLocation>newVivifyingLHMap_ATE();

  /**
   * The method's annotated lambda expressions; map key is the offset of the invokedynamic bytecode
   */
  public final VivifyingMap<RelativeLocation, AMethod> funs =
      new VivifyingMap<RelativeLocation, AMethod>(new LinkedHashMap<>()) {
        @Override
        public AMethod createValueFor(RelativeLocation k) {
          return new AMethod("" + k); // FIXME: find generated method name
        }

        @Override
        public boolean isEmptyValue(AMethod v) {
          return v.isEmpty();
        }
      };

  protected String id;

  AExpression(String id) {
    super(id);

    this.id = id;
  }

  AExpression(AExpression expr) {
    super(expr);
    copyMapContents(expr.typecasts, typecasts);
    copyMapContents(expr.instanceofs, instanceofs);
    copyMapContents(expr.news, news);
    copyMapContents(expr.calls, calls);
    copyMapContents(expr.refs, refs);
    copyMapContents(expr.funs, funs);
    id = expr.id;
  }

  @Override
  public AExpression clone() {
    // TODO: This does not populate the result!
    return new AExpression(id);
  }

  @Override
  public boolean equals(AElement o) {
    return o instanceof AExpression && ((AExpression) o).equalsExpression(this);
  }

  protected boolean equalsExpression(AExpression o) {
    return super.equals(o)
        && typecasts.equals(o.typecasts)
        && instanceofs.equals(o.instanceofs)
        && news.equals(o.news)
        && refs.equals(o.refs)
        && calls.equals(o.calls)
        && funs.equals(o.funs);
  }

  @Override
  public int hashCode() {
    return super.hashCode()
        + typecasts.hashCode()
        + instanceofs.hashCode()
        + news.hashCode()
        + refs.hashCode()
        + calls.hashCode()
        + funs.hashCode();
  }

  @Override
  public boolean isEmpty() {
    return super.isEmpty()
        && typecasts.isEmpty()
        && instanceofs.isEmpty()
        && news.isEmpty()
        && refs.isEmpty()
        && calls.isEmpty()
        && funs.isEmpty();
  }

  @Override
  public void prune() {
    super.prune();
    typecasts.prune();
    instanceofs.prune();
    news.prune();
    refs.prune();
    calls.prune();
    funs.prune();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    SortedMap<RelativeLocation, ATypeElement> map = new TreeMap<>();
    RelativeLocation prev = null;
    // sb.append("AExpression ");
    // sb.append(id);
    for (Map.Entry<RelativeLocation, ATypeElement> em : typecasts.entrySet()) {
      sb.append("typecast: ");
      RelativeLocation loc = em.getKey();
      sb.append(loc);
      sb.append(": ");
      AElement ae = em.getValue();
      sb.append(ae.toString());
      sb.append(' ');
    }
    for (Map.Entry<RelativeLocation, ATypeElement> em : instanceofs.entrySet()) {
      sb.append("instanceof: ");
      RelativeLocation loc = em.getKey();
      sb.append(loc);
      sb.append(": ");
      AElement ae = em.getValue();
      sb.append(ae.toString());
      sb.append(' ');
    }
    for (Map.Entry<RelativeLocation, ATypeElement> em : news.entrySet()) {
      sb.append("new ");
      RelativeLocation loc = em.getKey();
      sb.append(loc);
      sb.append(": ");
      AElement ae = em.getValue();
      sb.append(ae.toString());
      sb.append(' ');
    }
    map.putAll(refs);
    for (Entry<RelativeLocation, ATypeElement> em : map.entrySet()) {
      RelativeLocation loc = em.getKey();
      AElement ae = em.getValue();
      boolean isOffset = loc.index < 0;
      if (prev == null || (isOffset ? loc.offset == prev.offset : loc.index == prev.index)) {
        sb.append("reference ");
        sb.append(isOffset ? "*" + loc.offset : "#" + loc.index);
        sb.append(": ");
        sb.append(ae.toString());
      }
      if (loc.type_index >= 0) {
        sb.append("typearg " + loc);
        sb.append(": ");
        sb.append(ae.toString());
        sb.append(' ');
      }
      prev = loc;
    }
    prev = null;
    map.clear();
    map.putAll(calls);
    for (Entry<RelativeLocation, ATypeElement> em : map.entrySet()) {
      RelativeLocation loc = em.getKey();
      AElement ae = em.getValue();
      boolean isOffset = loc.index < 0;
      if (prev == null || (isOffset ? loc.offset == prev.offset : loc.index == prev.index)) {
        sb.append("call ");
        sb.append(isOffset ? "*" + loc.offset : "#" + loc.index);
        sb.append(": ");
      }
      if (loc.type_index >= 0) {
        sb.append("typearg " + loc);
        sb.append(": ");
        sb.append(ae.toString());
        sb.append(' ');
      }
      prev = loc;
    }
    prev = null;
    map.clear();
    for (Map.Entry<RelativeLocation, AMethod> em : funs.entrySet()) {
      sb.append("lambda ");
      RelativeLocation loc = em.getKey();
      sb.append(loc);
      sb.append(": ");
      AElement ae = em.getValue();
      sb.append(ae.toString());
      sb.append(' ');
    }
    return sb.toString();
  }

  @Override
  public <R, T> R accept(ElementVisitor<R, T> v, T t) {
    return v.visitExpression(this, t);
  }
}
