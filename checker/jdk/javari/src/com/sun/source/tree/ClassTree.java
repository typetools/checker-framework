package com.sun.source.tree;

import java.util.List;
import javax.lang.model.element.Name;
import org.checkerframework.checker.javari.qual.*;

public interface ClassTree extends StatementTree {
    @PolyRead ModifiersTree getModifiers(@PolyRead ClassTree this);
    @PolyRead Name getSimpleName(@PolyRead ClassTree this);
    @PolyRead List<? extends TypeParameterTree> getTypeParameters(@PolyRead ClassTree this);
    @PolyRead Tree getExtendsClause(@PolyRead ClassTree this);
    @PolyRead List<? extends Tree> getImplementsClause(@PolyRead ClassTree this);
    @PolyRead List<? extends Tree> getMembers(@PolyRead ClassTree this);
}
