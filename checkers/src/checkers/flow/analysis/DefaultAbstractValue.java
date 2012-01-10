package checkers.flow.analysis;

import java.util.Collections;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import checkers.flow.util.HashCodeUtils;
import checkers.types.QualifierHierarchy;
import checkers.util.AnnotationUtils;

/**
 * An implementation of an abstract value used by the default dataflow analysis.
 * Contains a set of annotations.
 * 
 * @author Stefan Heule
 * 
 */
public class DefaultAbstractValue implements AbstractValue {

	/**
	 * The qualifier hierarchy used to determine the least upper bound of two
	 * sets of annotations.
	 */
	protected QualifierHierarchy qualifierHierarchy;

	/** The annotation corresponding to this abstract value. */
	protected Set<AnnotationMirror> annotations;

	/**
	 * Create new default abstract value.
	 * 
	 * @param annotations
	 *            The set of annotations.
	 * @param qualifierHierarchy
	 *            The qualifier hierarchy used to determine the least upper
	 *            bound of two sets of annotations.
	 */
	public DefaultAbstractValue(Set<AnnotationMirror> annotations,
			QualifierHierarchy qualifierHierarchy) {
		this.annotations = annotations;
		this.qualifierHierarchy = qualifierHierarchy;
	}

	/** @return The annotations this abstract value stands for. */
	public Set<AnnotationMirror> getAnnotations() {
		return Collections.unmodifiableSet(annotations);
	}

	@Override
	public DefaultAbstractValue leastUpperBound(AbstractValue o) {
		assert o instanceof DefaultAbstractValue;
		DefaultAbstractValue other = (DefaultAbstractValue) o;
		Set<AnnotationMirror> lub = qualifierHierarchy.leastUpperBound(
				annotations, other.getAnnotations());
		return new DefaultAbstractValue(lub, qualifierHierarchy);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof DefaultAbstractValue)) {
			return false;
		}
		DefaultAbstractValue other = (DefaultAbstractValue) obj;
		return AnnotationUtils
				.areSame(getAnnotations(), other.getAnnotations());
	}

	@Override
	public int hashCode() {
		return HashCodeUtils.hash(annotations);
	}

	@Override
	public String toString() {
		return annotations.toString();
	}

}
