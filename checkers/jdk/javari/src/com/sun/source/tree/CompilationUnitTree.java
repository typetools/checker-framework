package com.sun.source.tree;

import java.util.List;
import javax.tools.JavaFileObject;
import com.sun.source.tree.LineMap;
import checkers.javari.quals.*;

public interface CompilationUnitTree extends Tree {
    @PolyRead List<? extends AnnotationTree> getPackageAnnotations(@PolyRead CompilationUnitTree this);
    @PolyRead ExpressionTree getPackageName(@PolyRead CompilationUnitTree this);
    @PolyRead List<? extends ImportTree> getImports(@PolyRead CompilationUnitTree this);
    @PolyRead List<? extends Tree> getTypeDecls(@PolyRead CompilationUnitTree this);
    @PolyRead JavaFileObject getSourceFile(@PolyRead CompilationUnitTree this);
    @PolyRead LineMap getLineMap(@PolyRead CompilationUnitTree this);
}
