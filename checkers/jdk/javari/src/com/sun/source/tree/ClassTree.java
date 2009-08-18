package com.sun.source.tree;

import java.util.List;
import javax.lang.model.element.Name;
import checkers.javari.quals.*;

public interface ClassTree extends StatementTree {
    @PolyRead ModifiersTree getModifiers() @PolyRead;
    @PolyRead Name getSimpleName() @PolyRead;
    @PolyRead List<? extends TypeParameterTree> getTypeParameters() @PolyRead;
    @PolyRead ModifiersTree getExtendsModifiers() @PolyRead;
    @PolyRead Tree getExtendsClause() @PolyRead;
    @PolyRead List<? extends ModifiersTree> getImplementsModifiers() @PolyRead;
    @PolyRead List<? extends Tree> getImplementsClause() @PolyRead;
    @PolyRead List<? extends Tree> getMembers() @PolyRead;
}
