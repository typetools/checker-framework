package org.checkerframework.checker.experimental.regex_qual;

import org.checkerframework.qualframework.base.Checker;

/**
 * Created by mcarthur on 6/3/14.
 */
public class RegexChecker extends Checker<Regex> {

    @Override
    protected RegexQualifiedTypeFactory createTypeFactory() {
        return new RegexQualifiedTypeFactory(this);
    }

}
