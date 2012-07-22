package checkers.commitment;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import checkers.basetype.BaseTypeChecker;
import checkers.commitment.quals.Committed;
import checkers.commitment.quals.Free;
import checkers.commitment.quals.NotOnlyCommitted;
import checkers.commitment.quals.Unclassified;
import checkers.quals.TypeQualifiers;
import checkers.util.AnnotationUtils;

@TypeQualifiers({ Free.class, Committed.class, Unclassified.class })
public abstract class CommitmentChecker extends BaseTypeChecker {

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
     * @return The list of type annotations of the commitment type system (i.e.,
     *         not including declaration annotations like {@link CommmittedOnly}
     *         or {@link NotOnlyCommitted}).
     */
    public Set<AnnotationMirror> getCommitmentAnnotations() {
        Set<AnnotationMirror> result = new HashSet<>();
        result.add(FREE);
        result.add(COMMITTED);
        result.add(UNCLASSIFIED);
        return result;
    }

    /*
     * The following method can be used to appropriately configure the
     * commitment type-system.
     */

    /**
     * @return The list of annotations that is forbidden for the constructor
     *         return type.
     */
    public Set<AnnotationMirror> getInvalidConstructorReturnTypeAnnotations() {
        return getCommitmentAnnotations();
    }

    /**
     * Returns the annotation that makes up the invariant of this commitment
     * type system.
     */
    public abstract AnnotationMirror getFieldInvariantAnnotation();

    /**
     * Returns a list of all fields of the given class
     */
    public static Set<VariableTree> getAllFields(ClassTree clazz) {
        Set<VariableTree> fields = new HashSet<>();
        for (Tree t : clazz.getMembers()) {
            if (t.getKind().equals(Tree.Kind.VARIABLE)) {
                VariableTree vt = (VariableTree) t;
                fields.add(vt);
            }
        }
        return fields;
    }
}
