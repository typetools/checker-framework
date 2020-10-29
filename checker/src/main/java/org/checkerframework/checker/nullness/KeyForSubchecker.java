package org.checkerframework.checker.nullness;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A type-checker for determining which values are keys for which maps. Typically used as part of
 * the compound checker for the nullness type system.
 *
 * @checker_framework.manual #map-key-checker Map Key Checker
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
public class KeyForSubchecker extends BaseTypeChecker {
    {
        // While strictly required for soundness, this leads to too many false positives.  Printing
        // a key or putting it in a map erases all knowledge of what maps it was currently a key
        // for.
        // TODO: Revisit when side effect annotations are more precise.
        // this.sideEffectsUnrefineAliases = true;
    }
}
