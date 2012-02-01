package com.sun.source.tree;

import java.util.List;
import javax.tools.JavaFileObject;
import com.sun.source.tree.LineMap;
import checkers.javari.quals.*;

public interface CompilationUnitTree extends Tree {
    @PolyRead List<? extends AnnotationTree> getPackageAnnotations() @PolyRead;
    @PolyRead ExpressionTree getPackageName() @PolyRead;
    @PolyRead List<? extends ImportTree> getImports() @PolyRead;
    @PolyRead List<? extends Tree> getTypeDecls() @PolyRead;
    @PolyRead JavaFileObject getSourceFile() @PolyRead;
    @PolyRead LineMap getLineMap() @PolyRead;
}
