@DefaultQualifier(
        value = H1S1.class,
        locations = {TypeUseLocation.LOCAL_VARIABLE},
        applyToSubpackages = false)
@DefaultQualifier(
        value = H2S1.class,
        locations = {TypeUseLocation.LOCAL_VARIABLE},
        applyToSubpackages = true)
package pkg1;

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2S1;
