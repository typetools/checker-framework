package checkers.flow.analysis;

import javax.lang.model.element.AnnotationMirror;

/**
 * An implementation of an asbtract value used by the default dataflow analysis.
 * 
 * @author Stefan Heule
 * 
 */
public class DefaultAbstractValue implements AbstractValue {

	/** The annotation corresponding to this abstract value. */
	// TODO: should be a set of annotations
	protected AnnotationMirror annotation;

	public DefaultAbstractValue(AnnotationMirror annotation) {
		this.annotation = annotation;
	}

	public AnnotationMirror getAnnotation() {
		return annotation;
	}
	
	@Override
	public DefaultAbstractValue leastUpperBound(AbstractValue o) {
		assert o instanceof DefaultAbstractValue;
		DefaultAbstractValue other = (DefaultAbstractValue) o;
		// TODO: correct implementation
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		return ((DefaultAbstractValue) obj).getAnnotation().equals(
				getAnnotation());
	}

	@Override
	public int hashCode() {
		return annotation.hashCode();
	}

	@Override
	public String toString() {
		return annotation.toString();
	}

}
