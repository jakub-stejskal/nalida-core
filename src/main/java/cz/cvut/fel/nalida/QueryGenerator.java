package cz.cvut.fel.nalida;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.KShortestPaths;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import com.google.common.collect.Sets;

import cz.cvut.fel.nalida.db.Element;
import cz.cvut.fel.nalida.db.Element.ElementType;
import cz.cvut.fel.nalida.db.Entity;
import cz.cvut.fel.nalida.db.QueryPlan;
import cz.cvut.fel.nalida.db.Schema;
import cz.cvut.fel.nalida.db.Subresource;

abstract public class QueryGenerator {
	protected final Schema schema;
	protected final Properties props;

	public QueryGenerator(Schema schema, Properties props) throws FileNotFoundException, IOException {
		this.schema = schema;
		this.props = props;
	}

	abstract public QueryPlan generateQuery(Tokenization tokenization);

	protected Set<Element> getProjectionElements(Tokenization tokenization) {

		Collection<Token> whWordTokens = tokenization.getTokens(ElementType.WH_WORD);
		if (whWordTokens.isEmpty()) {
			return Sets.newHashSet(tokenization.getRoot().getElement());
		} else {
			Token whToken = tokenization.getTokens(ElementType.WH_WORD).iterator().next();
			Set<Element> elements = new HashSet<>();
			for (Token token : tokenization.getAttached(whToken)) {
				elements.add(token.getElement());
			}
			return elements;
		}
	}

	protected Set<Entity> getEntityElements(Tokenization tokenization) {
		Set<Entity> entities = new HashSet<>();
		for (Token token : tokenization.getTokens()) {
			if (!token.isType(ElementType.WH_WORD)) {
				entities.add(token.getEntityElement());
			}
		}
		return entities;
	}

	protected Set<Token> getConstraintElements(Tokenization tokenization) {
		return new HashSet<>(tokenization.getTokens(ElementType.VALUE));
	}

	protected Entity getProjectionEntity(Set<Element> projections) {
		Set<Entity> resultEntities = new HashSet<>();
		for (Element projElement : projections) {
			if (projElement.isElementType(ElementType.SUBRESOURCE)) {
				resultEntities.add(((Subresource) projElement).getTypeEntity());
			} else {
				resultEntities.add(projElement.toEntityElement());
			}
		}
		if (resultEntities.size() > 1) {
			throw new UnsupportedOperationException("Multi-entity projections not supported.");
		}

		return resultEntities.iterator().next();
	}

	protected Entity getConstraintEntity(Set<Token> constraints, Entity projectionEntity) {
		Set<Entity> constraintEntities = new HashSet<>();
		for (Token constrToken : constraints) {
			if (constrToken.getEntityElement() != projectionEntity) {
				constraintEntities.add(constrToken.getEntityElement());
			}
		}
		if (constraintEntities.size() > 1) {
			throw new UnsupportedOperationException("Multiple non-result constraint entities not supported.");
		} else if (constraintEntities.size() == 0) {
			return projectionEntity;
		}
		return constraintEntities.iterator().next();
	}

	protected List<DefaultWeightedEdge> getShortestPath(Tokenization tokenization, Entity projEntity, Entity constrEntity) {
		if (projEntity.equals(constrEntity)) {
			return Collections.emptyList();
		}
		DirectedWeightedMultigraph<Element, DefaultWeightedEdge> graph = this.schema.getGraph();
		KShortestPaths<Element, DefaultWeightedEdge> paths = new KShortestPaths<>(graph, constrEntity, 5);
		List<Element> tokenizationElements = tokenization.getElements();
		Set<Entity> entities = getEntityElements(tokenization);

		double minPathWeight = Double.MAX_VALUE;
		List<DefaultWeightedEdge> minPath = null;
		for (GraphPath<Element, DefaultWeightedEdge> path : paths.getPaths(projEntity)) {
			double pathWeight = 0;
			for (DefaultWeightedEdge e : path.getEdgeList()) {
				boolean sourceInTokens = isInTokens(graph.getEdgeSource(e), tokenizationElements, entities);
				boolean targetInTokens = isInTokens(graph.getEdgeTarget(e), tokenizationElements, entities);

				if (!(sourceInTokens && targetInTokens)) {
					pathWeight += graph.getEdgeWeight(e);
				}
			}
			if (pathWeight < minPathWeight) {
				minPathWeight = pathWeight;
				minPath = path.getEdgeList();
			}
		}
		return minPath;
	}

	private boolean isInTokens(Element edgeNode, List<Element> tokenizationElements, Set<Entity> entities) {
		if (edgeNode.isElementType(ElementType.ENTITY)) {
			return entities.contains(edgeNode);
		} else {
			return tokenizationElements.contains(edgeNode);
		}
	}
}
