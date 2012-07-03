package checkers.javari;

import javax.lang.model.element.Element;

import java.util.Map;

import checkers.types.InternalUtils;

import com.sun.source.util.TreeScanner;
import com.sun.source.tree.*;

/**
 * Visitor used for creating mappings between trees and their elements, and
 * elements and their enclosing classes and methods.
 */
class TreeMapperVisitor extends TreeScanner<Void, MapperState> {

    private Map<Element, Tree> mapTree;
    private Map<Tree, ClassTree> mapClass;
    private Map<Tree, MethodTree> mapMethod;

    TreeMapperVisitor(Map<Element, Tree> mapTree,
                      Map<Tree, ClassTree> mapClass,
                      Map<Tree, MethodTree> mapMethod) {
        this.mapTree = mapTree;
        this.mapClass = mapClass;
        this.mapMethod = mapMethod;
    }

    @Override
    public Void scan(Tree node, MapperState p) {
        if (node != null) {
            Element symbol = InternalUtils.symbol(node);
            mapTree.put(symbol, node);
            if (p != null) {
                mapClass.put(node, p.classTree);
                mapMethod.put(node, p.methodTree);
            }
        }
        return super.scan(node, p);
    }

    @Override
    public Void visitClass(ClassTree node, MapperState p) {
        if (p == null)
            p = new MapperState(node);
        else
            p.classTree = node;

        return super.visitClass(node, p);
    }

    @Override
    public Void visitMethod(MethodTree node, MapperState p) {
        p.methodTree = node;
        return super.visitMethod(node, p);
    }

}
