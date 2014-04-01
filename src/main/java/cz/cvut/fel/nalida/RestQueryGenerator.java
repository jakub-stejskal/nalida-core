package cz.cvut.fel.nalida;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import com.google.common.collect.Iterables;

import cz.cvut.fel.nalida.db.Attribute;
import cz.cvut.fel.nalida.db.Element;
import cz.cvut.fel.nalida.db.Element.ElementType;
import cz.cvut.fel.nalida.db.Entity;
import cz.cvut.fel.nalida.db.QueryBuilder;
import cz.cvut.fel.nalida.db.QueryPlan;
import cz.cvut.fel.nalida.db.Schema;

public class RestQueryGenerator extends QueryGenerator {

	public RestQueryGenerator(Schema schema, Properties props) throws FileNotFoundException, IOException {
		super(schema, props);
	}

	@Override
	public QueryPlan generateQuery(Tokenization tokenization) {
		Set<Element> projections = getProjectionElements(tokenization);
		Set<Token> constraints = getConstraintElements(tokenization);
		Entity projectionEntity = getProjectionEntity(projections);
		Entity constraintEntity = getConstraintEntity(constraints, projectionEntity);
		DirectedWeightedMultigraph<Element, DefaultWeightedEdge> graph = this.schema.getGraph();
		List<DefaultWeightedEdge> path = getShortestPath(tokenization, projectionEntity, constraintEntity);

		QueryPlan plan = new QueryPlan();
		QueryBuilder query = new QueryBuilder(this.props);
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
						query = new QueryBuilder(this.props);
					} else {
						//						query.resource(attribute.toEntityElement().getResource());
						addConstraints(query, attribute.getName() + ".", entity, constraints);
					}
				} else if (target.isElementType(ElementType.SUBRESOURCE)) {
					query.resource(entity.getResource());
					addConstraints(query, "", entity, constraints);
					plan.addQuery(query.build());
					query = new QueryBuilder(this.props);
				} else {
					throw new UnsupportedOperationException("Unsupported connection: " + edge);
				}
			} else if (source.isElementType(ElementType.ATTRIBUTE)) {
				Attribute attribute = (Attribute) source;
				if (target.isElementType(ElementType.ENTITY)) {
					Entity entity = (Entity) target;
					if (attribute.toEntityElement().equals(entity)) {
						//TODO maybe nothing to do here?

					} else {
						query.resource("");
					}
				} else {
					throw new UnsupportedOperationException("Unsupported connection: " + edge);
				}
			} else if (target.isElementType(ElementType.SUBRESOURCE)) {
				//TODO
			} else {
				//TODO
			}
		}

		query.resource(projectionEntity.getResource());
		addConstraints(query, "", projectionEntity, constraints);
		for (Element projElement : projections) {
			if (!projElement.equals(projectionEntity)) {
				query.projection("content/" + projElement.getName());
			}
		}
		plan.addQuery(query.build());

		System.out.println("projec: " + projections);
		System.out.println("constr: " + constraints);
		System.out.println("path  : " + path);

		return plan;
	}

	private void addConstraints(QueryBuilder query, String constrAttribute, Entity constrEntity, Set<Token> constraints) {
		for (Token constrToken : constraints) {
			if (constrToken.getEntityElement().equals(constrEntity)) {
				query.constraint(constrAttribute + constrToken.getElementName(), "==",
						Iterables.toArray(constrToken.getWords(), String.class));
			}
		}
	}
}
