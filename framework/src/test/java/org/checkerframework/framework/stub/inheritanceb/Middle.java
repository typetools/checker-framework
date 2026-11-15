package org.checkerframework.framework.stub.inheritanceb;

import org.checkerframework.framework.stub.inheritancea.Base;

/**
 * A class that inherits the member type {@code Inner} from {@link I}, but not from {@link Base},
 * whose {@code Inner} is package-private in another package. Used by {@code
 * ToIndexFileConverterTest}.
 */
public class Middle extends Base implements I {}
