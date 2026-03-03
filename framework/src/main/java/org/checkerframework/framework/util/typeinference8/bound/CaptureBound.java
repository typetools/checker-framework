package org.checkerframework.framework.util.typeinference8.bound;

import com.sun.source.tree.ExpressionTree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint.Kind;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.Typing;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.CaptureVariable;
import org.checkerframework.framework.util.typeinference8.types.InferenceType;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.types.VariableBounds;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.TypesUtils;

/**
 * A bound of the form: {@code G<a1, ..., an> = capture(G<A1, ..., An>)}. The variables a1, ..., an
 * represent the result of capture conversion applied to {@code G<A1, ..., An>} (where A1, ..., An
 * may be types or wildcards and may mention inference variables).
 */
public class CaptureBound {
  /** {@code G<A1, ..., An>} sometimes called the right-hand side. */
  private final AbstractType capturedType;

  /**
   * The substitution [P1 := alpha1, ..., Pn := alphan] where P1, ..., Pn are the type parameters of
   * the underlying type, G.
   */
  private final Theta map;

  /** {@code G<a1, ..., an>} */
  private final InferenceType lhs;

  /** A list of {@link CaptureTuple}s. */
  private final List<CaptureTuple> tuples = new ArrayList<>();

  /**
   * All capture variables in this capture. For example, a1 in {@code G<a1, ..., an> = capture(G<A1,
   * ..., An>)}.
   */
  private final List<CaptureVariable> captureVariables = new ArrayList<>();

  /** Method invocation where variable is from. */
  private final ExpressionTree invocation;

  /**
   * Creates a captured bound.
   *
   * @param capturedType a capture type
   * @param invocation invocation a method or constructor invocation; used to create fresh inference
   *     variables
   * @param context the context
   */
  private CaptureBound(
      AbstractType capturedType, ExpressionTree invocation, Java8InferenceContext context) {
    this.capturedType = capturedType;
    this.invocation = invocation;
    DeclaredType underlying = (DeclaredType) capturedType.getJavaType();
    TypeElement ele = TypesUtils.getTypeElement(underlying);
    this.map = context.inferenceTypeFactory.createThetaForCapture(invocation, capturedType);

    lhs = (InferenceType) context.inferenceTypeFactory.getTypeOfElement(ele, map);

    Iterator<Variable> alphas = this.map.values().iterator();
    Iterator<AbstractType> args = capturedType.getTypeArguments().iterator();
    for (TypeParameterElement pEle : ele.getTypeParameters()) {
      AbstractType Bi = context.inferenceTypeFactory.getTypeOfBound(pEle, map);
      AbstractType Ai = args.next();

      CaptureVariable alphai = (CaptureVariable) alphas.next();
      captureVariables.add(alphai);
      alphai.initialBounds(map);

      tuples.add(CaptureTuple.of(alphai, Ai, Bi));
    }
  }

  /**
   * Given {@code r}, a parameterized type, {@code G<A1, ..., An>}}, and one of {@code A1, ..., An}
   * is a wildcard, then, for fresh inference variables {@code B1, ..., Bn}, the constraint formula
   * {@code <G<B1, ..., Bn> -> T>} is reduced and incorporated, along with the bound {@code G<B1,
   * ..., Bn> = capture(G<A1, ..., An>)}, with B2.
   *
   * @param r a parameterized type, {@code G<A1, ..., An>}, and one of {@code A1, ..., An} is a
   *     wildcard
   * @param target target of the constraint
   * @param invocation invocation a method or constructor invocation; used to create fresh inference
   *     variables
   * @param context the context
   * @return the result of incorporating the created capture constraint
   */
  public static BoundSet createAndIncorporateCaptureConstraint(
      AbstractType r,
      AbstractType target,
      ExpressionTree invocation,
      Java8InferenceContext context) {
    CaptureBound capture = new CaptureBound(r, invocation, context);
    return capture.incorporate(target, context);
  }

  /**
   * Incorporate this capture bound. See JLS 18.3.1.
   *
   * <p>Also, reduces and incorporates the constraint {@code G<a1,...,an> -> target}. See JLS
   * 18.5.2.1.
   *
   * @param target the target type of
   * @param context the context
   * @return the result of incorporation
   */
  private BoundSet incorporate(AbstractType target, Java8InferenceContext context) {
    // First add the non-wildcard bounds.
    for (CaptureTuple t : tuples) {
      if (t.capturedTypeArg.getTypeKind() != TypeKind.WILDCARD) {
        // If Ai is not a wildcard, then the bound alphai = Ai is implied.
        t.alpha.getBounds().addBound(null, VariableBounds.BoundKind.EQUAL, t.capturedTypeArg);
      }
    }

    String source = "Captured constraint from method call: " + invocation;
    ConstraintSet set = new ConstraintSet(new Typing(source, lhs, target, Kind.TYPE_COMPATIBILITY));
    // Reduce and incorporate so that the capture variables bounds are set.
    BoundSet b1 = set.reduce(context);
    b1.incorporateToFixedPoint(new BoundSet(context));

    // Then create constraints implied by captured type args that are wildcards.
    boolean containsFalse = false;
    for (CaptureTuple t : tuples) {
      if (t.capturedTypeArg.getTypeKind() == TypeKind.WILDCARD) {
        ConstraintSet newCon = t.alpha.getWildcardConstraints(t.capturedTypeArg, t.bound);
        if (newCon == null) {
          containsFalse = true;
        } else {
          set.addAll(newCon);
        }
      }
    }

    // Reduce and incorporate again.
    BoundSet b2 = set.reduce(context);
    b2.addCapture(this);
    if (containsFalse) {
      b2.addFalse();
    }
    b1.incorporateToFixedPoint(b2);
    return b1;
  }

  /**
   * Returns all variables on the left-hand side of this capture.
   *
   * @return all variables on the left-hand side of this capture
   */
  public List<? extends CaptureVariable> getAllVariablesOnLHS() {
    return captureVariables;
  }

  /**
   * Returns all variables on the right-hand side of this capture.
   *
   * @return all variables on the right-hand side of this capture
   */
  public Set<Variable> getAllVariablesOnRHS() {
    return new LinkedHashSet<>(capturedType.getInferenceVariables());
  }

  /**
   * Returns true if this bound contains any {@code variables}.
   *
   * @param variables inference variables
   * @return true if this bound contains any {@code variables}
   */
  public boolean isCaptureMentionsAny(Collection<Variable> variables) {
    for (Variable a : variables) {
      if (map.containsValue(a)) {
        return true;
      }
    }
    return false;
  }

  /**
   * For a capture of the form: {@code G<a1, ..., an> = capture(G<A1, ..., An>)}, a capture tuple
   * groups ai, Ai, and the upper bound of the corresponding type variable.
   */
  private static class CaptureTuple {

    /**
     * Fresh inference variable (in the left-hand side of the capture). (Also referred to as beta in
     * the some places in the JLS.) For example {@code a1} in {@code G<a1, ..., an> = capture(G<A1,
     * ..., An>)}.
     */
    public final CaptureVariable alpha;

    /**
     * Type argument in the right-hand side for the capture. For example {@code A1} in {@code G<a1,
     * ..., an> = capture(G<A1, ..., An>)}.
     */
    public final AbstractType capturedTypeArg;

    /**
     * Upper bound of one of the type parameters of G that has been substituted using the fresh
     * inference variables.
     */
    public final AbstractType bound;

    /**
     * Creates a tuple.
     *
     * @param alpha capture variable
     * @param capturedTypeArg captured type argument
     * @param bound the bound of the type parameter
     */
    private CaptureTuple(CaptureVariable alpha, AbstractType capturedTypeArg, AbstractType bound) {
      this.alpha = alpha;
      this.capturedTypeArg = capturedTypeArg;
      this.bound = bound;
    }

    /**
     * Creates a tuple.
     *
     * @param alpha capture variable
     * @param capturedTypeArg captured type argument
     * @param bound the bound of the type parameter
     * @return a tuple
     */
    public static CaptureTuple of(
        CaptureVariable alpha, AbstractType capturedTypeArg, AbstractType bound) {
      return new CaptureTuple(alpha, capturedTypeArg, bound);
    }
  }
}
