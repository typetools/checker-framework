package checkers.i18n;

import java.util.ArrayList;
import java.util.Collection;

import checkers.source.AggregateChecker;
import checkers.source.SourceChecker;

/**
 * A type-checker that enforces (and finds the violations of) two properties:
 *
 * <ol>
 * <li value="1">Only localized output gets emitted to the user</li>
 * <li value="2">Only localizable keys (i.e. keys found in localizing resource
 * bundles) get used as such.</li>
 * </ol>
 *
 * @see I18nSubchecker
 * @see LocalizableKeyChecker
 */
public class I18nChecker extends AggregateChecker {

    @Override
    protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
        Collection<Class<? extends SourceChecker>> checkers
            = new ArrayList<Class<? extends SourceChecker>>();
        checkers.add(I18nSubchecker.class);
        checkers.add(LocalizableKeyChecker.class);
        return checkers;
    }
}
