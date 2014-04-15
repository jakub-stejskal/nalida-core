package cz.cvut.fel.nalida.db;

import static java.lang.String.format;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.yaml.snakeyaml.Yaml;

@XmlRootElement
public final class Schema {
	private static final int DIRECT_EDGE_WEIGHT = 1;
	private static final double UNDIRECT_EDGE_WEIGHT = 1.1;
	private String baseUri;
	private List<Entity> entities;
	private DirectedWeightedMultigraph<Element, DefaultWeightedEdge> graph;

	public String getBaseUri() {
		return this.baseUri;
	}

	public void setBaseUri(String baseUri) {
		this.baseUri = baseUri;
	}

	@XmlElementWrapper(name = "entities")
	@XmlElement(name = "entity")
	public List<Entity> getEntities() {
		return this.entities;
	}

	public DirectedWeightedMultigraph<Element, DefaultWeightedEdge> getGraph() {
		return this.graph;
	}

	public void setEntities(List<Entity> entities) {
		this.entities = entities;
	}

	public Entity getEntityByName(String name) {
		for (Entity entity : this.entities) {
			if (entity.getName().equals(name)) {
				return entity;
			}
		}
		return null;
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		sb.append(format("BaseUri: %s\n", this.baseUri));
		sb.append(format("Entities: \n"));

		for (Entity entity : this.entities) {
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
		for (Entity entity : this.entities) {
			entityNames.put(entity.getName(), entity);
		}
		for (Entity entity : this.entities) {
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

		for (Entity entity : this.entities) {
			this.graph.addVertex(entity);
		}
		for (Entity entity : this.entities) {
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
