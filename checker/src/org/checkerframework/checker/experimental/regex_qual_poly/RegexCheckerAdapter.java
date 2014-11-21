package org.checkerframework.checker.experimental.regex_qual_poly;

import org.checkerframework.checker.experimental.regex_qual.Regex;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.qualframework.base.CheckerAdapter;
import org.checkerframework.qualframework.poly.QualParams;

/**
 * {@link CheckerAdapter} for the Regex-Qual-Param type system.
 */
public class RegexCheckerAdapter extends CheckerAdapter<QualParams<Regex>> {

    public RegexCheckerAdapter() {
        super(new RegexChecker());
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new RegexTypecheckVisitor(this);
    }
}
