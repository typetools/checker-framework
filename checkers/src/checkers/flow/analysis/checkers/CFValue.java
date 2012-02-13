package checkers.flow.analysis.checkers;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import checkers.flow.analysis.AbstractValue;
import checkers.flow.util.HashCodeUtils;
import checkers.util.AnnotationUtils;

/**
 * An implementation of an abstract value used by the Checker Framework dataflow
 * analysis. Contains a set of annotations.
 * 
 * @author Stefan Heule
 * 
 */
public class CFValue implements AbstractValue<CFValue> {

	/**
	 * The analysis class this store belongs to.
	 */
	protected final CFAnalysis analysis;

	/** The annotation corresponding to this abstract value. */
	protected Set<AnnotationMirror> annotations;

	public CFValue(CFAnalysis analysis, Set<AnnotationMirror> annotations) {
		this.analysis = analysis;
		assert areValidAnnotations(annotations);
		this.annotations = annotations;
	}

	/**
	 * Are the annotations {@code annotations} valid for the given analysis?
	 */
	protected boolean areValidAnnotations(Set<AnnotationMirror> annotations) {
		for (AnnotationMirror a : annotations) {
			if (!AnnotationUtils.containsSame(analysis.legalAnnotations, a)) {
				return false;
			}
		}
		return true;
	}

	/** @return The annotations this abstract value stands for. */
	public Set<AnnotationMirror> getAnnotations() {
		return annotations;
	}

	/**
	 * Computes and returns the least upper bound of two sets of type
	 * annotations. The return value is always of type
	 * DefaultTypeAnalysis.Value.
	 */
	@Override
	public CFValue leastUpperBound(CFValue other) {
		Set<AnnotationMirror> lub = analysis.typeHierarchy.leastUpperBound(
				annotations, other.annotations);
		return new CFValue(analysis, lub);
	}

	/**
	 * Return whether this Value is a proper subtype of the argument Value.
	 */
	public boolean isSubtypeOf(CFValue other) {
		return analysis.typeHierarchy.isSubtype(annotations, other.annotations);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof CFValue)) {
			return false;
		}
		CFValue other = (CFValue) obj;
		return AnnotationUtils
				.areSame(getAnnotations(), other.getAnnotations());
	}

	@Override
	public int hashCode() {
		return HashCodeUtils.hash(annotations);
	}

	/**
	 * @return The string representation as a comma-separated list of simple
	 *         annotation names.
	 */
	@Override
	public String toString() {
		List<String> l = new LinkedList<>();
		for (AnnotationMirror a : annotations) {
			DeclaredType annoType = a.getAnnotationType();
			TypeElement elm = (TypeElement) annoType.asElement();
			l.add(elm.getSimpleName().toString());
		}
		String s = l.toString();
		return s.substring(1, s.length() - 1);
	}
}