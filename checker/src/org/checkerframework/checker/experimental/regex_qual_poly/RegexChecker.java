package org.checkerframework.checker.experimental.regex_qual_poly;

import org.checkerframework.qualframework.base.Checker;
import org.checkerframework.qualframework.poly.QualParams;

/**
 * Created by mcarthur on 6/3/14.
 */
public class RegexChecker extends Checker<QualParams<Regex>> {

    @Override
    protected RegexQualifiedTypeFactory createTypeFactory() {
        return new RegexQualifiedTypeFactory(this);
    }

}
