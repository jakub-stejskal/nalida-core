package cz.cvut.fel.nalida.db;

import static java.lang.String.format;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.yaml.snakeyaml.Yaml;

public final class Schema {
	private static final int DIRECT_EDGE_WEIGHT = 1;
	private static final double UNDIRECT_EDGE_WEIGHT = 1.1;
	private String version;
	private List<Entity> schema;
	private DirectedWeightedMultigraph<Element, DefaultWeightedEdge> graph;

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public List<Entity> getSchema() {
		return this.schema;
	}

	public DirectedWeightedMultigraph<Element, DefaultWeightedEdge> getGraph() {
		return this.graph;
	}

	public void setSchema(List<Entity> schema) {
		this.schema = schema;
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		sb.append(format("Version: %s\n", this.version));
		sb.append(format("Schema: \n"));

		for (Entity entity : this.schema) {
			sb.append(entity.toStringDeep() + "\n");
		}

		return sb.toString();
	}

	public static Schema load(InputStream input) {
		Schema s = new Yaml().loadAs(input, Schema.class);
		s.linkReferences();
		s.createGraph();
		return s;
	}

	private Schema linkReferences() {
		Map<String, Entity> entityNames = new HashMap<>();
		for (Entity entity : this.schema) {
			entityNames.put(entity.getName(), entity);
		}
		for (Entity entity : this.schema) {
			for (Attribute attribute : entity.getAttributes()) {
				attribute.parent = entity;
				if (!attribute.isPrimitiveType()) {
					String typeName = attribute.getType();
					if (attribute.isCollectionType()) {
						String typeNameWithoutStar = typeName.substring(0, typeName.length() - 1);
						attribute.setTypeEntity(entityNames.get(typeNameWithoutStar));
					} else {
						attribute.setTypeEntity(entityNames.get(attribute.getType()));
					}
				}
			}
			for (Attribute attribute : entity.getSubresources()) {
				attribute.parent = entity;
				if (!attribute.isPrimitiveType()) {
					attribute.setTypeEntity(entityNames.get(attribute.getType()));
				}
			}
		}
		return this;
	}

	private void createGraph() {
		this.graph = new DirectedWeightedMultigraph<>(DefaultWeightedEdge.class);

		for (Entity entity : this.schema) {
			this.graph.addVertex(entity);
		}
		for (Entity entity : this.schema) {
			for (Attribute attribute : entity.getAttributes()) {
				if (!attribute.isPrimitiveType()) {
					Entity type = attribute.getTypeEntity();
					this.graph.addVertex(attribute);
					this.graph.setEdgeWeight(this.graph.addEdge(entity, attribute), DIRECT_EDGE_WEIGHT);
					this.graph.setEdgeWeight(this.graph.addEdge(attribute, type), UNDIRECT_EDGE_WEIGHT);

					if (!attribute.isCollectionType()) {
						this.graph.setEdgeWeight(this.graph.addEdge(attribute, entity), DIRECT_EDGE_WEIGHT);
						this.graph.setEdgeWeight(this.graph.addEdge(type, attribute), DIRECT_EDGE_WEIGHT);
					}
				}
			}
			for (Attribute attribute : entity.getSubresources()) {
				if (!attribute.isPrimitiveType()) {
					Entity type = attribute.getTypeEntity();
					this.graph.addVertex(attribute);
					this.graph.setEdgeWeight(this.graph.addEdge(entity, attribute), DIRECT_EDGE_WEIGHT);
					this.graph.setEdgeWeight(this.graph.addEdge(attribute, type), UNDIRECT_EDGE_WEIGHT);
				}
			}
		}
	}
}
