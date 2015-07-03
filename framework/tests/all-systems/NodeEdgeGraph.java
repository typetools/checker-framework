/*
 * Test case for Issue 116:
 * https://github.com/typetools/checker-framework/issues/116
 */

class Node<EdgeType extends Edge<? extends Node<EdgeType>>> {}

class Edge<NodeType extends Node<? extends Edge<NodeType>>> {}

// The first two lines are already enough to trigger one bug. The next line reveals another.
class Graph<GrNodeType extends Node<GrEdgeType>, GrEdgeType extends Edge<GrNodeType>> {}
