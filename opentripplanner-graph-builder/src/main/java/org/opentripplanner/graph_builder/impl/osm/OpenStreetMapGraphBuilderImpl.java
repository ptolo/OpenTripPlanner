package org.opentripplanner.graph_builder.impl.osm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opentripplanner.graph_builder.model.osm.OSMNode;
import org.opentripplanner.graph_builder.model.osm.OSMRelation;
import org.opentripplanner.graph_builder.model.osm.OSMWay;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapContentHandler;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapProvider;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.SameInAndOutVertex;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.vertextypes.Intersection;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class OpenStreetMapGraphBuilderImpl implements GraphBuilder {

    private static final GeometryFactory _geometryFactory = new GeometryFactory();
    
    private List<OpenStreetMapProvider> _providers = new ArrayList<OpenStreetMapProvider>();

    public void setProvider(OpenStreetMapProvider provider) {
        _providers.add(provider);
    }

    public void setProviders(List<OpenStreetMapProvider> providers) {
        _providers.addAll(providers);
    }

    @Override
    public void buildGraph(Graph graph) {
        Handler handler = new Handler();
        for (OpenStreetMapProvider provider : _providers)
            provider.readOSM(handler);
        handler.buildGraph(graph);
    }

    private class Handler implements OpenStreetMapContentHandler {

        private Map<Integer, OSMNode> _nodes = new HashMap<Integer, OSMNode>();

        private Map<Integer, OSMWay> _ways = new HashMap<Integer, OSMWay>();

        public void buildGraph(Graph graph) {

            // We want to prune nodes that don't have any edges
            Set<Integer> nodesWithNeighbors = new HashSet<Integer>();

            for (OSMWay way : _ways.values()) {
                List<Integer> nodes = way.getNodeRefs();
                if (nodes.size() > 1)
                    nodesWithNeighbors.addAll(nodes);
            }

            // Remove all island
            _nodes.keySet().retainAll(nodesWithNeighbors);

            for (OSMNode node : _nodes.values()) {
                int nodeId = node.getId();
                String id = getVertexIdForNodeId(nodeId);
                Vertex vertex = graph.getVertex(id);

                if (vertex != null)
                    throw new IllegalStateException("osm node already loaded: id=" + id);

                vertex = new SameInAndOutVertex(id, Intersection.class, node.getLon(), node
                        .getLat());
                graph.addVertex(vertex);

            }

            for (OSMWay way : _ways.values()) {
                StreetTraversalPermission permissions = getPermissionsForWay(way);
                List<Integer> nodes = way.getNodeRefs();
                for (int i = 0; i < nodes.size() - 1; i++) {
                    String vFromId = getVertexIdForNodeId(nodes.get(i));
                    String vToId = getVertexIdForNodeId(nodes.get(i + 1));
                    SameInAndOutVertex from = (SameInAndOutVertex) graph.getVertex(vFromId);
                    SameInAndOutVertex to = (SameInAndOutVertex) graph.getVertex(vToId);
                    if (from == null || to == null)
                        continue;
                    double d = from.distance(to);
                    from.addEdge(getEdgeForStreet(from, to, way, d, permissions));
                    to.addEdge(getEdgeForStreet(to, from, way, d, permissions));
                }
            }

        }

        public void addNode(OSMNode node) {

            if (_nodes.containsKey(node.getId()))
                return;

            _nodes.put(node.getId(), node);
        }

        public void addWay(OSMWay way) {
            if (_ways.containsKey(way.getId()))
                return;
            _ways.put(way.getId(), way);
        }

        public void addRelation(OSMRelation relation) {

        }

        private String getVertexIdForNodeId(int nodeId) {
            return "osm node " + nodeId;
        }

        private Street getEdgeForStreet(SameInAndOutVertex from, SameInAndOutVertex to, OSMWay way, double d,
                StreetTraversalPermission permissions) {
            
            String id = "way " + way.getId();
            Street street = new Street(from, to, id, id, d, permissions);
            
            LineString lineString = _geometryFactory.createLineString(new Coordinate[] { from.getCoordinate(), to.getCoordinate()});
            street.setGeometry(lineString);
            
            return street;
        }

        private StreetTraversalPermission getPermissionsForWay(OSMWay way) {

            // TODO : Better mapping between OSM tags and travel permissions

            Map<String, String> tags = way.getTags();
            String value = tags.get("highway");

            if (value == null || value.equals("motorway") || value.equals("motorway_link"))
                return StreetTraversalPermission.CAR_ONLY;

            return StreetTraversalPermission.ALL;
        }
    }
}