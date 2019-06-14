package org.checkerframework.common.subtyping;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.processing.SupportedOptions;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.SourceVisitor;

/**
 * A checker for type qualifier systems that only checks subtyping relationships.
 *
 * <p>The annotation(s) are specified on the command line, using an annotation processor argument:
 *
 * <ul>
 *   <li>{@code -Aquals}: specifies the annotations in the qualifier hierarchy (as a comma-separated
 *       list of fully-qualified annotation names with no spaces in between). Only the annotation
 *       for one qualified subtype hierarchy can be passed.
 * </ul>
 *
 * @checker_framework.manual #subtyping-checker Subtying Checker
 */
@SupportedOptions({"quals", "qualDirs"})
public final class SubtypingChecker extends BaseTypeChecker {

    /**
     * Compute getSuppressWarningsKeys, based on the names of all the qualifiers.
     *
     * <p>Provided for the convenience of checkers that do not subclass Subtyping Checker (because
     * it is final). Clients should call it like:
     *
     * <pre>{@code
     * SubtypingChecker.getSuppressWarningsKeys(this.visitor, super.getSuppressWarningsKeys());
     * }</pre>
     *
     * @param visitor the visitor
     * @param superSupportedTypeQualifiers the result of super.getSuppressWarningsKeys(), as
     *     executed by checker
     */
    public static Collection<String> getSuppressWarningsKeys(
            SourceVisitor<?, ?> visitor, Collection<String> superSupportedTypeQualifiers) {
        TreeSet<String> result = new TreeSet<>(superSupportedTypeQualifiers);
        result.add(SUPPRESS_ALL_KEY); // Is this always already in superSupportedTypeQualifiers?

        Set<Class<? extends Annotation>> annos =
                ((BaseTypeVisitor<?>) visitor).getTypeFactory().getSupportedTypeQualifiers();
        for (Class<? extends Annotation> anno : annos) {
            result.add(anno.getSimpleName().toLowerCase());
        }

        return result;
    }

    @Override
    public Collection<String> getSuppressWarningsKeys() {
        return getSuppressWarningsKeys(this.visitor, super.getSuppressWarningsKeys());
    }
}
