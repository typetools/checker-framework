package org.checkerframework.framework.stub.inheritanceb;

import org.checkerframework.framework.stub.inheritancea.ProtectedBase;

/**
 * A class on the classpath, used by {@code ToIndexFileConverterTest}, that inherits a protected
 * member type from a class in another package and a public member type of the same name from an
 * interface.
 */
public class ProtectedSubWithInterface extends ProtectedBase implements ProtectedInnerInterface {}
