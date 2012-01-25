package checkers.basic;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.annotation.processing.*;

import checkers.basetype.BaseTypeChecker;
import checkers.types.*;

import com.sun.source.tree.CompilationUnitTree;

/**
 * A checker for type qualifier systems that only checks subtyping
 * relationships.
 *
 * <p>
 *
 * The annotation(s) are specified on the command line, using an annotation
 * processor argument:
 *
 * <ul>
 * <li>{@code -Aquals}: specifies the annotations in the qualifier hierarchy
 * (as a comma-separated list of fully-qualified annotation names with no
 * spaces in between).  Only the annotation for one qualified subtype
 * hierarchy can be passed.</li>
 * </ul>
 *
 */
@SupportedOptions( { "quals" })
public final class BasicChecker extends BaseTypeChecker {

    @Override
    public AnnotatedTypeFactory createFactory(CompilationUnitTree root) {
        return new BasicAnnotatedTypeFactory<BasicChecker>(this, root);
    }

    @Override @SuppressWarnings("unchecked")
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {

        String qualNames = env.getOptions().get("quals");
        if (qualNames == null) {
            errorAbort("BasicChecker: missing required option: -Aquals");
        }

        Set<Class<? extends Annotation>> qualSet =
            new HashSet<Class<? extends Annotation>>();
        for (String qualName : qualNames.split(",")) {
            try {
                final Class<? extends Annotation> q =
                    (Class<? extends Annotation>)Class.forName(qualName);
                qualSet.add(q);
            } catch (ClassNotFoundException e) {
                errorAbort("BasicChecker: could not load class for qualifier: " + qualName + "; ensure that your classpath is correct.");
            }
        }

        return Collections.unmodifiableSet(qualSet);
    }

    @Override
    public Collection<String> getSuppressWarningsKey() {
        Set<String> swKeys = new HashSet<String>();
        Set<Class<? extends Annotation>> annos = getSupportedTypeQualifiers();
        if (annos.isEmpty())
            return super.getSuppressWarningsKey();

        for (Class<? extends Annotation> anno : annos)
            swKeys.add(anno.getSimpleName().toLowerCase());

        return swKeys;
    }
}
