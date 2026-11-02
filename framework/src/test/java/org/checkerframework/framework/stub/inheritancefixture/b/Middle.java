package org.checkerframework.framework.stub.inheritancefixture.b;

import org.checkerframework.framework.stub.inheritancefixture.a.Base;

/**
 * A class in a different package than its superclass, so it does not inherit {@code Base.Member}.
 * For ToIndexFileConverterTest.
 */
public class Middle extends Base {}
