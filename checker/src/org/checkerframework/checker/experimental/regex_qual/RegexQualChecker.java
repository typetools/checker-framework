package org.checkerframework.checker.experimental.regex_qual;

import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.qualframework.base.Checker;

/**
 * {@link Checker} for the Regex-Qual type system.
 */
@StubFiles("apache-xerces.astub")
public class RegexQualChecker extends Checker<Regex> {

    @Override
    protected RegexQualifiedTypeFactory createTypeFactory() {
        return new RegexQualifiedTypeFactory(this);
    }
}
