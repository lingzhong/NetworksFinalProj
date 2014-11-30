// Graph node and edge class implementations to represent a computer network
public class GraphNode implements Comparable<GraphNode> {
    public final int name;
    public Edge[] neighbors;
    public double minDistance = Double.POSITIVE_INFINITY;
    public GraphNode previous; // to keep the path for backtracking

    public GraphNode(int argName) {
        this.name = argName;
    }

    @Override
    public int compareTo(GraphNode other) {
        return Double.compare(this.minDistance, other.minDistance);
    }
}

class Edge {
    public final GraphNode target;
    public final double weight;

    public Edge(GraphNode argTarget, double argWeight) {
        this.target = argTarget;
        this.weight = argWeight;
    }
}