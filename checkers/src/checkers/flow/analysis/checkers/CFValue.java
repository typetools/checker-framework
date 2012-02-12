package checkers.flow.analysis.checkers;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import checkers.flow.analysis.AbstractValue;

/**
 * An abstact value for the default analysis is a set of annotations from the
 * QualifierHierarchy.
 */
public class CFValue implements AbstractValue<CFValue> {
	
	/**
	 * The analysis class this store belongs to.
	 */
	protected final CFAnalysis analysis;
	
	
	private Set<AnnotationMirror> annotations;

	CFValue(CFAnalysis analysis) {
		this.analysis = analysis;
		annotations = new HashSet<AnnotationMirror>();
	}

	CFValue(CFAnalysis analysis, Set<AnnotationMirror> annotations) {
		this.analysis = analysis;
		this.annotations = annotations;
	}

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
	boolean isSubtypeOf(CFValue other) {
		return analysis.typeHierarchy.isSubtype(annotations, other.annotations);
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