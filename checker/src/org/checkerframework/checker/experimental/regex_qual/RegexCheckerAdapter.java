package org.checkerframework.checker.experimental.regex_qual;

import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.qualframework.base.CheckerAdapter;

/**
 * {@link CheckerAdapter} for the Regex-Qual type system.
 */
public class RegexCheckerAdapter extends CheckerAdapter<Regex> {

    public RegexCheckerAdapter() {
        super(new RegexQualChecker());
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new RegexTypecheckVisitor(this);
    }
}
