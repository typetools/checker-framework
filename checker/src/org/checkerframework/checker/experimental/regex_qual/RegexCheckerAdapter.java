package org.checkerframework.checker.experimental.regex_qual;

import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.qualframework.base.CheckerAdapter;

/**
 * Created by mcarthur on 6/3/14.
 */
public class RegexCheckerAdapter extends CheckerAdapter<Regex> {

    public RegexCheckerAdapter() {
        super(new RegexChecker());
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new RegexTypecheckVisitor(this);
    }
}
