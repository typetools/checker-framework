package checkers.localizing;

import java.util.ArrayList;
import java.util.Collection;

import checkers.source.SourceChecker;
import checkers.util.AggregateChecker;

/**
 * A type-checker that enforces (and finds the violations) two properties:
 *
 * <ol>
 * <li value="1">Only localized output gets emitted to the user</li>
 * <li value="2">Only localizable keys (i.e. keys found in localizing resource
 * bundles) get used as such.</li>
 * </ol>
 *
 * @see LocalizingChecker
 * @see KeyLookupChecker
 */
public class FullLocalizationChecker extends AggregateChecker {

    @Override
    protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
        Collection<Class<? extends SourceChecker>> checkers
            = new ArrayList<Class<? extends SourceChecker>>();
        checkers.add(LocalizingChecker.class);
        checkers.add(KeyLookupChecker.class);
        return checkers;
    }
}
