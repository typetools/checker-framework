package org.checkerframework.checker.compilermsgs;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.propkey.PropertyKeyChecker;
import org.checkerframework.checker.propkey.qual.PropertyKey;
import org.checkerframework.checker.propkey.qual.PropertyKeyBottom;
import org.checkerframework.checker.propkey.qual.UnknownPropertyKey;
import org.checkerframework.framework.qual.TypeQualifiers;

/**
 * A PropertyKeyChecker for the compiler message keys that are used
 * in the Checker framework.
 *
 * @author wmdietl
 */
@TypeQualifiers({ CompilerMessageKey.class, PropertyKey.class,
    UnknownPropertyKey.class, PropertyKeyBottom.class })
public class CompilerMessagesChecker extends PropertyKeyChecker {}
