package checkers.units;


import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.SupportedOptions;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.units.quals.*;
import checkers.basetype.BaseTypeChecker;

/**
 * Units Checker main class.
 * 
 * Supports "units" option to add support for additional units.
 */
@SupportedOptions( { "units" } )
public class UnitsChecker extends BaseTypeChecker {
    /** Copied from BasicChecker and adapted "quals" to "units".
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        Set<Class<? extends Annotation>> qualSet =
            new HashSet<Class<? extends Annotation>>();

        String qualNames = env.getOptions().get("units");
        if (qualNames == null) {
        } else {
          try {
            for (String qualName : qualNames.split(",")) {
              final Class<? extends Annotation> q =
                (Class<? extends Annotation>) Class.forName(qualName);
              qualSet.add(q);
            }
          } catch (ClassNotFoundException e) {
            throw new Error(e);
          }
        }
        
        // Only add the directly supported units. Shorthands like kg are
        // handled automatically by aliases.
        
        qualSet.add(Length.class);
        // qualSet.add(mm.class);
        // qualSet.add(Meter.class);
        qualSet.add(m.class);
        // qualSet.add(km.class);
        
        qualSet.add(Time.class);
        // qualSet.add(Second.class);
        qualSet.add(s.class);
        qualSet.add(min.class);
        qualSet.add(h.class);
        
        qualSet.add(Speed.class);
        qualSet.add(mPERs.class);
        qualSet.add(kmPERh.class);
        
        qualSet.add(Area.class);
        qualSet.add(mm2.class);
        qualSet.add(m2.class);
        qualSet.add(km2.class);
        
        qualSet.add(Current.class);
        qualSet.add(A.class);
        
        qualSet.add(Mass.class);
        qualSet.add(g.class);
        // qualSet.add(kg.class);
        
        qualSet.add(Substance.class);
        qualSet.add(mol.class);
        
        qualSet.add(Luminance.class);
        qualSet.add(cd.class);
        
        qualSet.add(Temperature.class);
        qualSet.add(C.class);
        qualSet.add(K.class);
        
        return Collections.unmodifiableSet(qualSet);
    }

    /** Copied from BasicChecker; cannot reuse it, because BasicChecker is final.
     */
    @Override
    public Collection<String> getSuppressWarningsKey() {
        Set<String> swKeys = new HashSet<String>();
        Set<Class<? extends Annotation>> annos = getSupportedTypeQualifiers();
        if (annos.isEmpty())
            return super.getSuppressWarningsKey();

        for (Class<? extends Annotation> anno : annos)
            swKeys.add(anno.getSimpleName().toLowerCase());

        return swKeys;
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType) {
        // The checker calls this method to compare the annotation used in a
        // type to the modifier it adds to the class declaration. As our default
        // modifier is Unqualified, this results in an error when a non-subtype
        // is used. Just ignore this check here and do them manually in the
        // visitor.
        return true;
    }

}
