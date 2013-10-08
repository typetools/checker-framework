package checkers.subtyping;

import checkers.basetype.BaseTypeChecker;
import checkers.basetype.BaseTypeVisitor;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.SupportedOptions;

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
 * @checker.framework.manual #subtyping-checker Subtying Checker
 */
@SupportedOptions( { "quals" })
public final class SubtypingChecker extends BaseTypeChecker {
    @Override
    public Collection<String> getSuppressWarningsKeys() {
        Set<String> swKeys = new HashSet<String>();
        Set<Class<? extends Annotation>> annos = ((BaseTypeVisitor<?>)visitor).getTypeFactory().getSupportedTypeQualifiers();
        if (annos.isEmpty())
            return super.getSuppressWarningsKeys();

        for (Class<? extends Annotation> anno : annos)
            swKeys.add(anno.getSimpleName().toLowerCase());

        return swKeys;
    }
}
