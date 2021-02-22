/*
 * Test case for Issue 116:
 * https://github.com/typetools/checker-framework/issues/116
 */

class Issue116Node<EdgeType extends Issue116Edge<? extends Issue116Node<EdgeType>>> {}

class Issue116Edge<NodeType extends Issue116Node<? extends Issue116Edge<NodeType>>> {}

// The first two lines are already enough to trigger one bug. The next line reveals another.
public class Issue116Graph<
        GrNodeType extends Issue116Node<GrEdgeType>, GrEdgeType extends Issue116Edge<GrNodeType>> {}
