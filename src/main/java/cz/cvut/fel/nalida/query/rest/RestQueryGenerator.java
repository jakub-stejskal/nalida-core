package cz.cvut.fel.nalida.query.rest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import com.google.common.collect.Iterables;

import cz.cvut.fel.nalida.interpretation.Interpretation;
import cz.cvut.fel.nalida.interpretation.Token;
import cz.cvut.fel.nalida.query.QueryGenerator;
import cz.cvut.fel.nalida.query.QueryPlan;
import cz.cvut.fel.nalida.schema.Attribute;
import cz.cvut.fel.nalida.schema.Element;
import cz.cvut.fel.nalida.schema.Element.ElementType;
import cz.cvut.fel.nalida.schema.Entity;
import cz.cvut.fel.nalida.schema.Schema;

public class RestQueryGenerator extends QueryGenerator {

	public RestQueryGenerator(Schema schema, Properties props) throws FileNotFoundException, IOException {
		super(schema, props);
		props.put("baseUrl", schema.getBaseUri());
	}

	@Override
	public QueryPlan generateQuery(Interpretation interpretation) {
		Set<Element> projections = getProjectionElements(interpretation);
		Set<Token> constraints = getConstraintElements(interpretation);
		Entity projectionEntity = getProjectionEntity(projections);
		Set<Entity> constraintEntity = getConstraintEntities(constraints, projectionEntity);
		DirectedWeightedMultigraph<Element, DefaultWeightedEdge> graph = this.schema.getGraph();
		List<DefaultWeightedEdge> path = getShortestPath(interpretation, projectionEntity, constraintEntity);

		QueryPlan plan = new QueryPlan();
		RestQueryBuilder query = new RestQueryBuilder(this.props);
		for (DefaultWeightedEdge edge : path) {
			Element source = graph.getEdgeSource(edge);
			Element target = graph.getEdgeTarget(edge);
			if (source.isElementType(ElementType.ENTITY)) {
				Entity entity = (Entity) source;
				if (target.isElementType(ElementType.ATTRIBUTE)) {
					Attribute attribute = (Attribute) target;
					if (attribute.toEntityElement().equals(entity)) {
						query.resource(entity.getResource());
						query.projection("content/" + attribute.getName());
						addConstraints(query, "", entity, constraints);
						plan.addQuery(query.build());
						query = new RestQueryBuilder(this.props);
					} else {
						addConstraints(query, attribute.getName() + ".", entity, constraints);
					}
				} else if (target.isElementType(ElementType.SUBRESOURCE)) {
					query.resource(entity.getResource());
					addConstraints(query, "", entity, constraints);
					plan.addQuery(query.build());
					query = new RestQueryBuilder(this.props);
					query.resource(target.getName() + "/");
				} else {
					throw new UnsupportedOperationException("Unsupported connection: " + edge);
				}
			} else if (source.isElementType(ElementType.ATTRIBUTE)) {
				Attribute attribute = (Attribute) source;
				if (target.isElementType(ElementType.ENTITY)) {
					Entity entity = (Entity) target;
					if (attribute.toEntityElement().equals(entity)) {
					} else {
						query.resource("");
						query.collection(false);
					}
				} else {
					throw new UnsupportedOperationException("Unsupported connection: " + edge);
				}
			} else if (source.isElementType(ElementType.SUBRESOURCE)) {
			} else {
			}
		}
		query.resource(projectionEntity.getResource());
		addConstraints(query, "", projectionEntity, constraints);
		for (Element projElement : projections) {
			if (projElement.isElementType(ElementType.ATTRIBUTE)) {
				query.projection("content/" + projElement.getName());
			}
		}
		plan.addQuery(query.build());
		return plan;
	}

	private void addConstraints(RestQueryBuilder query, String constrAttribute, Entity constrEntity, Set<Token> constraints) {
		for (Token constrToken : constraints) {
			if (constrToken.getEntityElement().equals(constrEntity)) {
				if (!isTokenOfAttrType(constrToken, "string")) {
					assert constrToken.getWords().size() == 1;
					query.constraintExact(constrAttribute + constrToken.getElementName(), "==", constrToken.getWords().get(0).toUpperCase());
				} else {
					query.constraint(constrAttribute + constrToken.getElementName(), "==",
							Iterables.toArray(constrToken.getWords(), String.class));
				}
			}
		}
	}
}
