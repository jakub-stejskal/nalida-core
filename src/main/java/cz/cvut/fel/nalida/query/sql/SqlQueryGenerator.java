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

import cz.cvut.fel.nalida.query.QueryGenerator;
import cz.cvut.fel.nalida.query.QueryPlan;
import cz.cvut.fel.nalida.schema.Element;
import cz.cvut.fel.nalida.schema.Element.ElementType;
import cz.cvut.fel.nalida.schema.Entity;
import cz.cvut.fel.nalida.schema.Schema;
import cz.cvut.fel.nalida.tokenization.Token;
import cz.cvut.fel.nalida.tokenization.Tokenization;

public class SqlQueryGenerator extends QueryGenerator {

	public SqlQueryGenerator(Schema schema, Properties props) throws FileNotFoundException, IOException {
		super(schema, props);
	}

	@Override
	public QueryPlan generateQuery(Tokenization tokenization) {
		Set<String> entities = getEntities(tokenization);
		Set<String> projection = getProjection(tokenization);
		Set<String> constraints = getConstraints(tokenization);
		Set<String> joins = getJoinConstraints(tokenization);
		SqlQuery query = new SqlQuery(entities, projection, Sets.union(constraints, joins));

		QueryPlan qp = new QueryPlan();
		qp.addQuery(query);
		return qp;
	}

	private Set<String> getEntities(Tokenization tokenization) {
		Set<String> entities = new HashSet<>();
		for (Entity entityElement : getEntityElements(tokenization)) {
			entities.add(entityElement.getName());
		}
		return entities;
	}

	private Set<String> getProjection(Tokenization tokenization) {
		Set<String> projection = new HashSet<>();
		for (Element projElement : getProjectionElements(tokenization)) {
			String attributeName = projElement.isElementType(ElementType.ENTITY) ? "*" : projElement.getName();
			projection.add(projElement.toEntityElement().getName() + "." + attributeName);
		}
		return projection;
	}

	private Set<String> getConstraints(Tokenization tokenization) {
		Set<String> constraints = new HashSet<>();
		for (Token constrToken : getConstraintElements(tokenization)) {
			StringBuilder sb = new StringBuilder();
			sb.append(constrToken.getEntityElement().getName() + "." + constrToken.getElementName());
			sb.append(" LIKE '%");
			sb.append(Joiner.on("%").join(constrToken.getWords()));
			sb.append("%'");
			constraints.add(sb.toString());
		}
		return constraints;
	}

	private Set<String> getJoinConstraints(Tokenization tokenization) {
		Set<Element> projections = getProjectionElements(tokenization);
		Set<Token> constraints = getConstraintElements(tokenization);
		Entity projectionEntity = getProjectionEntity(projections);
		Set<Entity> constraintEntities = getConstraintEntities(constraints, projectionEntity);

		List<DefaultWeightedEdge> path = getShortestPath(tokenization, projectionEntity, constraintEntities);
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
