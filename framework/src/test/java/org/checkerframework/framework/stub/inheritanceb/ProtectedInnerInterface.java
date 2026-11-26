package org.checkerframework.framework.stub.inheritanceb;

/**
 * An interface with a member type of the same name as a protected member type of {@link
 * org.checkerframework.framework.stub.inheritancea.ProtectedBase}. Used by {@code
 * ToIndexFileConverterTest}.
 */
public interface ProtectedInnerInterface {
  /** A member type, which is implicitly public. */
  class ProtectedInner {}
}
