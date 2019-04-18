package com.got.genealogy.core.graph;

import com.got.genealogy.core.graph.collection.AdjacencyList;
import com.got.genealogy.core.graph.collection.AdjacencyMatrix;
import com.got.genealogy.core.graph.property.Edge;
import com.got.genealogy.core.graph.property.Vertex;
import com.got.genealogy.core.graph.property.Weight;
import com.got.genealogy.core.graph.property.WeightedVertex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class Graph<Vert extends Vertex, Arc extends Edge> {

    private AdjacencyMatrix<Weight<Arc>> matrix;
    private Map<Vert, Integer> vertices;
    private boolean directed;

    public Graph() {
        this(false);
    }

    public Graph(boolean directed) {
        matrix = new AdjacencyMatrix<>();
        vertices = new HashMap<>();
        this.directed = directed;
    }

    public AdjacencyMatrix<Weight<Arc>> getAdjacencyMatrix() {
        return matrix;
    }

    public Map<Vert, Integer> getVertices() {
        return vertices;
    }

    public Arc getEdge(String label1, String label2) {
        Vert vertex1 = getVertex(label1);
        Vert vertex2 = getVertex(label2);
        return getEdge(vertex1, vertex2);
    }

    public Arc getEdge(Vert vertex1, Vert vertex2) {
        Weight<Arc> edge = getEdgeWeighted(vertex1, vertex2);
        if (edge != null) {
            return edge.getWeight();
        }
        return null;
    }

    public Weight<Arc> getEdgeWeighted(String label1, String label2) {
        Vert vertex1 = getVertex(label1);
        Vert vertex2 = getVertex(label2);
        return getEdgeWeighted(vertex1, vertex2);
    }

    public Weight<Arc> getEdgeWeighted(Vert vertex1, Vert vertex2) {
        if (existingVertex(vertex1, vertex2)) {
            int fromVertex = vertices.get(vertex1);
            int toVertex = vertices.get(vertex2);
            return matrix.getCell(fromVertex, toVertex);
        }
        return null;
    }

    public void addEdge(Vert vertex1, Vert vertex2) {
        addEdge(vertex1, vertex2, new Weight<>());
    }

    public void addEdge(Vert vertex1, Vert vertex2, Weight<Arc> weight) {
        if (existingVertex(vertex1, vertex2)) {
            // Get index numbers of vertices
            int fromVertex = vertices.get(vertex1);
            int toVertex = vertices.get(vertex2);
            // Add outgoing edge
            matrix.setCell(fromVertex, toVertex, weight);
            if (!directed) {
                // Add incoming edge
                matrix.setCell(toVertex, fromVertex, weight);
            }
        }
    }

    public void removeEdge(Vert vertex1, Vert vertex2) {
        addEdge(vertex1, vertex2, null);
    }

    public Vert getVertex(int index) {
        for (Map.Entry<Vert, Integer> vertex : vertices.entrySet()) {
            if (vertex.getValue().equals(index)) {
                return vertex.getKey();
            }
        }
        return null;
    }

    public Vert getVertex(String label) {
        for (Map.Entry<Vert, Integer> vertex : vertices.entrySet()) {
            if (vertex.getKey().getLabel().equals(label)) {
                return vertex.getKey();
            }
        }
        return null;
    }

    public void addVertex(Vert vertex) {
        if (!existingVertex(vertex)) {
            // Add vertex with new index
            vertices.put(vertex, vertices.size());
            int newIndex = matrix.size();
            // Add new column to existing rows
            matrix.addColumn(null);
            // Add new empty row
            matrix.addRow();
            // Fill new row
            matrix.fillRow(newIndex, null);
        }
    }

    public void removeVertex(Vert vertex) {
        if (existingVertex(vertex)) {
            Integer index = vertices.get(vertex);
            // Remove and shift left
            vertices.remove(vertex);
            vertices.replaceAll((k, v) -> {
                return (v >= index) ? v - 1 : v;
            });
            // Remove from both axes
            matrix.removeRow(index);
            matrix.removeColumn(index);
        }
    }

    public boolean isAdjacent(Vert vertex1, Vert vertex2) {
        return getEdge(vertex1, vertex2) != null;
    }

    public Set<Vert> adjacentVertices(Vert vertex) {
        if (existingVertex(vertex)) {
            int vertexIndex = vertices.get(vertex);
            Set<Vert> adjacentVertices = new HashSet<>();
            List<Weight<Arc>> row, column;

            row = matrix.getRow(vertexIndex);
            column = matrix.getColumn(vertexIndex);

            for (int i = 0; i < row.size(); i++) {
                for (Map.Entry<Vert, Integer> vertexItem : vertices.entrySet()) {
                    // TODO: Replace with LinkedHashMap
                    // Get vertex with corresponding
                    // index in the HashMap.
                    if (containsAdjacent(row, vertexItem.getValue(), i)) {
                        adjacentVertices.add(vertexItem.getKey());
                    }
                    // Matrix rows and columns have
                    // the same size. If not directed
                    // then look at column too. Column
                    // collection is null is not
                    // directed.
                    if (directed && containsAdjacent(column, vertexItem.getValue(), i)) {
                        // Adjacent if incoming or
                        // outgoing from vertex.
                        adjacentVertices.add(vertexItem.getKey());
                    }
                }
            }
            return adjacentVertices;
        }
        return null;
    }

    /**
     * Convert the AdjacencyMatrix into
     * an AdjacencyList, with weighted
     * vertices, to store the weight
     * between connected nodes.
     * Only looking at rows in matrix.
     *
     * @return AdjacencyList of vertices
     * adjacent to weighted vertices.
     */
    public AdjacencyList<Vert, Arc> adjacencyListWeighted() {
        AdjacencyList<Vert, Arc> adjacencyList = new AdjacencyList<>();
        // For each vertex, check adjacent
        // vertices and attach a weight to
        // them.
        vertices.forEach((vertex, vertexIndex) -> {
            List<Weight<Arc>> row = matrix.getRow(vertexIndex);
            Set<WeightedVertex<Vert, Arc>> adjacentVertices = new HashSet<>();
            // Get edge and add to set
            for (Map.Entry<Vert, Integer> vertexItem : vertices.entrySet()) {
                Vert adjacentVertex = vertexItem.getKey();
                Arc weight = getEdge(vertex, adjacentVertex);
                if (weight != null) {
                    adjacentVertices.add(
                            new WeightedVertex<>(
                                    adjacentVertex,
                                    weight));
                }
            }
            adjacencyList.put(vertex, adjacentVertices);
        });
        return adjacencyList;
    }

    /**
     * Use DFS traversal to see if any
     * path exists between two vertices.
     *
     * @param vertex1 Starting vertex.
     * @param vertex2 Goal vertex.
     * @return Boolean after running DFS
     * traversal.
     */
    public boolean pathExistsBetween(Vert vertex1, Vert vertex2) {
        return depthFirstTraversal(vertex1).contains(vertex2);
    }

    /**
     * Depth-first traversal of the graph
     * from a starting vertex.
     *
     * @param vertex Starting vertex.
     * @return ArrayList of vertices,
     * in order of the path.
     */
    public List<Vert> depthFirstTraversal(Vert vertex) {
        Stack<Vert> stack = new Stack<>();
        List<Vert> path = new ArrayList<>();
        // Un-visit all vertices before
        // traversing. Push starting
        // vertex onto the stack.
        vertices.forEach((k, v) -> k.setVisited(false));
        stack.push(vertex);
        while (!stack.empty()) {
            Vert topVertex = stack.pop();
            if (!topVertex.isVisited()) {
                path.add(topVertex);
                topVertex.setVisited(true);
                // Safely iterate over
                // adjacent vertices
                // (regardless of direction)
                // and push unvisited onto
                // stack.
                Iterator<Vert> adjacentVertices = adjacentVertices(topVertex).iterator();
                while (adjacentVertices.hasNext()) {
                    Vert adjacentVertex = adjacentVertices.next();
                    if (!adjacentVertex.isVisited()) {
                        // Push unvisited neighbour
                        stack.push(adjacentVertex);
                    }
                }
            }
        }
        return path;
    }

    /**
     * Use an inner class to initialise
     * a shortestPath ArrayList and update
     * its contents based on the result
     * from a recursive relative-traversal
     * method.
     *
     * @param vertex1 Starting point, for
     *                graph traversal.
     * @param vertex2 Goal vertex, to reach
     *                after traversing all
     *                relatives of vertex1.
     * @return Return the shortestPath
     * of the inner class,
     * after graph traversal.
     */
    public List<Vert> getShortestUnweightedPath(Vert vertex1, Vert vertex2) {
        // Need to use an inner class, to use
        // with the adjacentVertices() method
        // from the inherited Graph class.
        class PathProcessor {
            // Can directly access shortestPath
            // outside this nested class.
            private List<Vert> shortestPath = new ArrayList<>();

            /**
             * Prepare an empty directed path list and
             * call processAllPaths to update the
             * shortestPath.
             *
             * @param vertex1   Starting point for
             *                  graph traversal.
             * @param vertex2   Goal vertex, to reach
             *                  after traversing.
             */
            private PathProcessor(Vert vertex1, Vert vertex2) {
                List<Vert> tempPath = new ArrayList<>();
                tempPath.add(vertex1);
                processAllPaths(vertex1, vertex2, tempPath);
            }

            /**
             * Recursive path processor. Goes
             * through all possible paths from
             * vertex1 to vertex2 and updates the
             * shortestPath if a shortest path exists.
             *
             * @param vertex1   Starting point for
             *                  for graph traversal.
             *                  Every relative of
             *                  initial vertex is
             *                  recursively traversed.
             * @param vertex2   Goal vertex, to reach
             *                  after traversing.
             *                  Unvisited when reached,
             *                  to search for other
             *                  possible paths.
             * @param currentPath   Path-so-far tracker.
             */
            private void processAllPaths(Vert vertex1,
                                         Vert vertex2,
                                         List<Vert> currentPath) {
                vertex1.setVisited(true);
                if (vertex1.equals(vertex2)) {
                    // Need to un-visit node to
                    // look for other possible paths.
                    vertex1.setVisited(false);
                    boolean shorterPath = currentPath.size() < this.shortestPath.size();
                    boolean emptyGlobalPath = this.shortestPath.size() == 0;

                    if (shorterPath || emptyGlobalPath) {
                        // Update shortest path variable
                        this.shortestPath = new ArrayList<>(currentPath);
                    }
                }
                // Get all relatives and
                // traverse graph over all
                // unvisited relatives.
                Iterator<Vert> adjacentVertices = adjacentVertices(vertex1).iterator();
                while (adjacentVertices.hasNext()) {
                    Vert adjacentVertex = adjacentVertices.next();
                    if (!adjacentVertex.isVisited()) {
                        currentPath.add(adjacentVertex);
                        // Recursively traverse neighbours
                        processAllPaths(adjacentVertex, vertex2, currentPath);
                        currentPath.remove(adjacentVertex);
                    }
                }
                vertex1.setVisited(false);
            }
        }
        return new PathProcessor(vertex1, vertex2).shortestPath;
    }

    private boolean existingVertex(Vert vertex) {
        // Todo: check if can have same strings
        return vertices.get(vertex) != null;
    }

    private boolean existingVertex(Vert vertex1, Vert vertex2) {
        return existingVertex(vertex1) && existingVertex(vertex2);
    }

    /**
     * Shortcut, used in adjacentVertices.
     * Works only if weights collection
     * matches the number of existing
     * vertices.
     *
     * @param weights        list of weights,
     *                       the order of which
     *                       should match that
     *                       of the matrix.
     * @param vertexPosition position of vertex.
     * @param index          used to get position
     *                       from weights and to
     *                       compare against row
     *                       or column position.
     * @return a boolean after comparing the row
     * or column position with its position in
     * a list of weights.
     */
    private boolean containsAdjacent(List<Weight<Arc>> weights,
                                     int vertexPosition,
                                     int index) {
        if (weights.size() == vertices.size()) {
            return weights.get(index) != null && vertexPosition == index;
        }
        return false;
    }
}