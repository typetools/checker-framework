package org.checkerframework.checker.units;

import java.util.SortedSet;
import javax.annotation.processing.SupportedOptions;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.subtyping.SubtypingChecker;

/**
 * Units Checker main class.
 *
 * <p>Supports "units" option to add support for additional individually named and externally
 * defined units, and "unitsDirs" option to add support for directories of externally defined units.
 * Directories must be well-formed paths from file system root, separated by colon (:) between each
 * directory.
 *
 * @checker_framework.manual #units-checker Units Checker
 */
@SupportedOptions({"units", "unitsDirs"})
public class UnitsChecker extends BaseTypeChecker {

    @Override
    public SortedSet<String> getSuppressWarningsPrefixes() {
        return SubtypingChecker.getSuppressWarningsPrefixes(
                this.visitor, super.getSuppressWarningsPrefixes());
    }
}
