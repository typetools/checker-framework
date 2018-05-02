package org.checkerframework.common.subtyping;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.processing.SupportedOptions;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

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
    @Override
    public Collection<String> getSuppressWarningsKeys() {
        Set<Class<? extends Annotation>> annos =
                ((BaseTypeVisitor<?>) visitor).getTypeFactory().getSupportedTypeQualifiers();
        if (annos.isEmpty()) {
            return super.getSuppressWarningsKeys();
        }

        Set<String> swKeys = new HashSet<>();
        swKeys.add(SUPPRESS_ALL_KEY);
        for (Class<? extends Annotation> anno : annos) {
            swKeys.add(anno.getSimpleName().toLowerCase());
        }

        return swKeys;
    }
}
