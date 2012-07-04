package checkers.commitment;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import checkers.basetype.BaseTypeChecker;
import checkers.commitment.quals.Committed;
import checkers.commitment.quals.Free;
import checkers.commitment.quals.NotOnlyCommitted;
import checkers.commitment.quals.Unclassified;
import checkers.quals.TypeQualifiers;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.util.AnnotationUtils;

@TypeQualifiers({ Free.class, Committed.class, Unclassified.class })
public class CommitmentChecker extends BaseTypeChecker {

	/** Annotation constants */
	protected AnnotationMirror COMMITTED, FREE, UNCLASSIFIED,
			NOT_ONLY_COMMITTED;

	@Override
	public void initChecker(ProcessingEnvironment processingEnv) {
		super.initChecker(processingEnv);
		AnnotationUtils annoFactory = AnnotationUtils.getInstance(env);

		COMMITTED = annoFactory.fromClass(Committed.class);
		FREE = annoFactory.fromClass(Free.class);
		UNCLASSIFIED = annoFactory.fromClass(Unclassified.class);
		NOT_ONLY_COMMITTED = annoFactory.fromClass(NotOnlyCommitted.class);
	}

	/**
	 * @return The list of annotations of the commitment type system (not
	 *         including {@link CommmittedOnly} and {@link NotOnlyCommitted}.
	 */
	public List<AnnotationMirror> getCommitmentAnnotations() {
		return Arrays.asList(new AnnotationMirror[] { FREE, COMMITTED,
				UNCLASSIFIED });
	}

	@Override
	public boolean isValidUse(AnnotatedDeclaredType declarationType,
			AnnotatedDeclaredType useType) {
		Set<AnnotationMirror> annotations = useType.getAnnotations();
		boolean ok = false;

		// there needs to be exactly one non-null annotation
		for (AnnotationMirror a : getCommitmentAnnotations()) {
			if (annotations.contains(a)) {
				if (ok) {
					return false;
				}
				ok = true;
			}
		}

		return ok;
	}
}
