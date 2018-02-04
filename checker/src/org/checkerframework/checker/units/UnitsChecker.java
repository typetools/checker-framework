package org.checkerframework.checker.units;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.processing.SupportedOptions;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/**
 * Units Checker main class.
 *
 * <p>Provides "units" option to support the use of externally defined, individually named units.
 * The units must be named using their fully qualified annotation names, separated by commas. E.g.
 * {@code -Aunits=A,myPackage.B} .
 *
 * <p>Provides "unitsDirs" option to support the use of externally defined units identified by the
 * directory they are contained in. Directories must be well-formed paths from file system root,
 * separated by colon (:) between each directory. E.g. {@code
 * -AunitsDirs=/path/to/qual:/path/to/otherquals} .
 *
 * <p>Also provides "unitsRelations" option to support the use of externally defined multiplication
 * and division units relationships, which are subclasses of {@link
 * org.checkerframework.checker.units.UnitsRelations}. These subclasses must be named using their
 * fully qualified class names, separated by commas. E.g. {@code
 * -AunitsRelations=SomeRelations,myPackage.OtherRelations} .
 *
 * @checker_framework.manual #units-checker Units Checker
 */
@SupportedOptions({"units", "unitsDirs", "unitsRelations"})
public class UnitsChecker extends BaseTypeChecker {

    /*
    @Override
    public void initChecker() {
        super.initChecker();
    }
    */

    /**
     * Copied from SubtypingChecker; cannot reuse it, because SubtypingChecker is final. TODO:
     * SubtypingChecker might also want to always call super.
     */
    @Override
    public Collection<String> getSuppressWarningsKeys() {
        Set<String> swKeys = new HashSet<String>(super.getSuppressWarningsKeys());
        Set<Class<? extends Annotation>> annos =
                ((BaseTypeVisitor<?>) visitor).getTypeFactory().getSupportedTypeQualifiers();

        for (Class<? extends Annotation> anno : annos) {
            swKeys.add(anno.getSimpleName().toLowerCase());
        }

        return swKeys;
    }
}
