package cz.cvut.fel.nalida.query.sql;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

import cz.cvut.fel.nalida.interpretation.Interpretation;
import cz.cvut.fel.nalida.interpretation.Token;
import cz.cvut.fel.nalida.query.QueryGenerator;
import cz.cvut.fel.nalida.query.QueryPlan;
import cz.cvut.fel.nalida.schema.Element;
import cz.cvut.fel.nalida.schema.Element.ElementType;
import cz.cvut.fel.nalida.schema.Entity;
import cz.cvut.fel.nalida.schema.Schema;

public class SqlQueryGenerator extends QueryGenerator {

	public SqlQueryGenerator(Schema schema, Properties props) throws FileNotFoundException, IOException {
		super(schema, props);
	}

	@Override
	public QueryPlan generateQuery(Interpretation interpretation) {
		Set<String> entities = getEntities(interpretation);
		Set<String> projection = getProjection(interpretation);
		Set<String> constraints = getConstraints(interpretation);
		Set<String> joins = getJoinConstraints(interpretation);
		SqlQuery query = new SqlQuery(entities, projection, Sets.union(constraints, joins));

		QueryPlan qp = new QueryPlan();
		qp.addQuery(query);
		return qp;
	}

	private Set<String> getEntities(Interpretation interpretation) {
		Set<String> entities = new HashSet<>();
		for (Entity entityElement : getEntityElements(interpretation)) {
			entities.add(entityElement.getName());
		}
		return entities;
	}

	private Set<String> getProjection(Interpretation interpretation) {
		Set<String> projection = new HashSet<>();
		for (Element projElement : getProjectionElements(interpretation)) {
			String attributeName = projElement.isElementType(ElementType.ENTITY) ? "*" : projElement.getName();
			projection.add(projElement.toEntityElement().getName() + "." + attributeName);
		}
		return projection;
	}

	private Set<String> getConstraints(Interpretation interpretation) {
		Set<String> constraints = new HashSet<>();
		for (Token constrToken : getConstraintElements(interpretation)) {
			StringBuilder sb = new StringBuilder();
			sb.append(constrToken.getEntityElement().getName() + "." + constrToken.getElementName());
			sb.append(" LIKE '%");
			sb.append(Joiner.on("%").join(constrToken.getWords()));
			sb.append("%'");
			constraints.add(sb.toString());
		}
		return constraints;
	}

	private Set<String> getJoinConstraints(Interpretation interpretation) {
		Set<Element> projections = getProjectionElements(interpretation);
		Set<Token> constraints = getConstraintElements(interpretation);
		Entity projectionEntity = getProjectionEntity(projections);
		Set<Entity> constraintEntities = getConstraintEntities(constraints, projectionEntity);

		List<DefaultWeightedEdge> path = getShortestPath(interpretation, projectionEntity, constraintEntities);
		System.out.println("PATH: " + path);
		return getJoins(path);
	}

	private Set<String> getJoins(List<DefaultWeightedEdge> path) {
		DirectedWeightedMultigraph<Element, DefaultWeightedEdge> graph = this.schema.getGraph();
		HashSet<String> joins = new HashSet<String>();
		for (DefaultWeightedEdge edge : path) {
			Element source = graph.getEdgeSource(edge);
			Element target = graph.getEdgeTarget(edge);
			if (!source.toEntityElement().equals(target.toEntityElement())) {
				joins.add(elementString(source) + "=" + elementString(target));
			}
		}
		return joins;
	}

	private String elementString(Element element) {
		if (element.isElementType(ElementType.ENTITY)) {
			return element.getLongName() + ".id";
		} else {
			return element.getLongName();
		}
	}
}
