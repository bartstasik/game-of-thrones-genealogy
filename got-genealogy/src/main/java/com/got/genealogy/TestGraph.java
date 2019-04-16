package com.got.genealogy;

import com.got.genealogy.core.family.person.Relation;
import com.got.genealogy.core.graph.Graph;
import com.got.genealogy.core.graph.property.Edge;
import com.got.genealogy.core.graph.property.Vertex;
import com.got.genealogy.core.graph.property.Weight;

import java.util.Set;

public class TestGraph {

    public static void main(String[] args) {
        Graph<Vertex, Edge> graph1 = new Graph<>(false);

        Vertex a = new Vertex("a");
        Vertex b = new Vertex("b");
        Vertex c = new Vertex("c");
        Vertex d = new Vertex("d");

        // Makes it more readable when
        // null is replaced with 0.
        Edge defaultNull = new Edge("0");

        graph1.addVertex(a);
        graph1.addVertex(b);
        graph1.addVertex(c);
        graph1.addVertex(d);

        graph1.addEdge(a, b,
                new Weight<>(new Edge("1")));
        graph1.addEdge(a, c,
                new Weight<>(new Edge("2")));
        graph1.addEdge(b, d,
                new Weight<>(new Edge("3")));

        System.out.println("\n\nGraph 1");
        graph1.printGraph(defaultNull);

        graph1.removeVertex(c);

        System.out.println("\n\nRemoved vertex [c]");
        graph1.printGraph(defaultNull);

        graph1.addVertex(c);

        System.out.println("\n\nAdded vertex [c]");
        graph1.printGraph(defaultNull);

        graph1.removeEdge(a, b);

        System.out.println("\n\nRemoved edge [a -> b]");
        graph1.printGraph(defaultNull);

        graph1.addEdge(c, d, new Weight<>(new Relation("4")));

        System.out.println("\n\nAdded edge [c -> d]");
        graph1.printGraph(defaultNull);

        System.out.println("\n\nGet [a] annotation: " + a.getAnnotation());
        a.setAnnotation("A");
        System.out.println("Set [a] annotation: " + a.getAnnotation());

        System.out.println("\n\n[a] visited: " + a.isVisited());
        graph1.visitVertex(a);
        System.out.println("\nVisited [a]");
        System.out.println("[a] visited: " + a.isVisited());
        System.out.println("\nLeft [a]");
        graph1.leaveVertex(a);
        System.out.println("[a] visited: " + a.isVisited());

        System.out.println("\nEdge [c -> d]: " + graph1.getEdge(c, d).getLabel());
        System.out.println("Edge [c -> d]: " + graph1.getEdgeWeighted(c, d).getWeight().getLabel());
        System.out.println("Vertex 2: " + graph1.getVertex(2).getLabel());

        Set<Vertex> adjacentToD = graph1.adjacentVertices(d);
        System.out.print("\n[d] adjacent vertices <expected: b, c>: ");
        adjacentToD.forEach((e) -> System.out.print(e.getLabel() + ", "));
        System.out.println();

        Graph<Vertex, Edge> graph2 = new Graph<>();

        graph2.addVertex(a);
        graph2.addVertex(b);
        graph2.addVertex(c);
        graph2.addVertex(d);

        graph2.addEdge(a, b,
                new Weight<>(new Edge("1")));
        graph2.addEdge(a, c,
                new Weight<>(new Edge("1")));
        graph2.addEdge(d, a,
                new Weight<>(new Edge("1")));

        graph2.printGraph(defaultNull);

        System.out.println("\nEdge [a -> b]: " + graph2.getEdge(a, b).getLabel());
        System.out.println("Edge [a -> c]: " + graph2.getEdge(a, c).getLabel());
        System.out.println("Edge [d -> a]: " + graph2.getEdge(d, a).getLabel());

        Set<Vertex> adjacentToA = graph2.adjacentVertices(a);
        System.out.print("\n[a] adjacent vertices <expected: b, c, d>: ");
        adjacentToA.forEach((e) -> System.out.print(e.getLabel() + ", "));

        Set<Vertex> adjacentToD2 = graph2.adjacentVertices(d);
        System.out.print("\n[d] adjacent vertices <expected: a>: ");
        adjacentToD2.forEach((e) -> System.out.print(e.getLabel() + ", "));

        System.out.println();
    }
}
