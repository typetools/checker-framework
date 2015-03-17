package org.checkerframework.checker.oigj;

import javax.lang.model.element.Element;

import org.checkerframework.checker.oigj.qual.Dominator;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;

import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

public class OwnershipVisitor extends BaseTypeVisitor<OwnershipAnnotatedTypeFactory> {

    public OwnershipVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType, Tree tree) {
        return true;
    }

    @Override
    protected boolean isAccessAllowed(Element field,
            AnnotatedTypeMirror receiver, ExpressionTree accessTree) {
        AnnotatedTypeMirror fType = atypeFactory.getAnnotatedType(field);
        if (fType.hasAnnotation(Dominator.class)
            && !atypeFactory.isMostEnclosingThisDeref(accessTree))
            return false;
        return super.isAccessAllowed(field, receiver, accessTree);
    }
    @Override
    protected void checkExceptionParameter(CatchTree node) {
        // TODO: Top is @World, but the default is bottom, @Modifier.
        // If exception parameters are set to @World then
        // the all-systems/UnionTypes.java fails:
        //  } catch (ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException exc) {
        //     Exception e = exc;  // assignment types incompatible.  found: @World required: @Modifier
        //  }
        // and  all-systems/SimpleLog.java fails:
        //   } catch (Exception e) {
        //         throw new RuntimeException ("", e); // argument types incompatible.  found: @World required: @Modifier
        //   }
        // It might be sound to force all thrown exceptions to be @Modifer, but since there is no
        // documentation on what @Modifer means, I'm not sure.
        // For now, don't enable this check
    }
}
