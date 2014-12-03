package org.checkerframework.checker.experimental.regex_qual_poly;

import org.checkerframework.checker.experimental.regex_qual.Regex;
import org.checkerframework.qualframework.base.Checker;
import org.checkerframework.qualframework.poly.QualParams;

/**
 * {@link Checker} for the Regex-Qual-Param type system.
 */
public class RegexQualPolyChecker extends Checker<QualParams<Regex>> {

    @Override
    protected RegexQualifiedTypeFactory createTypeFactory() {
        return new RegexQualifiedTypeFactory(this);
    }

}
