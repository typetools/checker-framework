package lib;

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.framework.qual.HasQualifierParameter;

/**
 * Carries an {@code @Inherited} declaration annotation and has a supertype whose type argument's
 * class file is absent, so reading this class's annotations is what fails. See Issue8055.java.
 */
@HasQualifierParameter(Tainted.class)
public class QualParam extends Box<Missing> {}
