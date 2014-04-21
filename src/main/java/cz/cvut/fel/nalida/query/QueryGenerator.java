package cz.cvut.fel.nalida.query;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jgrapht.Graphs;
import org.jgrapht.alg.FloydWarshallShortestPaths;
import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import cz.cvut.fel.nalida.schema.Element;
import cz.cvut.fel.nalida.schema.Element.ElementType;
import cz.cvut.fel.nalida.schema.Entity;
import cz.cvut.fel.nalida.schema.Schema;
import cz.cvut.fel.nalida.schema.Subresource;
import cz.cvut.fel.nalida.tokenization.Token;
import cz.cvut.fel.nalida.tokenization.Tokenization;

abstract public class QueryGenerator {
	protected final Schema schema;
	protected final Properties props;

	class ElementGraph extends DirectedWeightedMultigraph<Element, DefaultWeightedEdge> {

		private static final long serialVersionUID = 1L;
		private final List<Element> tokenizationElements;
		private final Set<Entity> entities;

		public ElementGraph(DirectedWeightedMultigraph<Element, DefaultWeightedEdge> graph, List<Element> tokenElements,
				Set<Entity> entities) {
			super(new ClassBasedEdgeFactory<Element, DefaultWeightedEdge>(DefaultWeightedEdge.class));
			Graphs.addGraph(this, graph);
			this.tokenizationElements = tokenElements;
			this.entities = entities;

		}

		@Override
		public double getEdgeWeight(DefaultWeightedEdge e) {
			boolean sourceInTokens = isInTokens(getEdgeSource(e), this.tokenizationElements, this.entities);
			boolean targetInTokens = isInTokens(getEdgeTarget(e), this.tokenizationElements, this.entities);
			if (sourceInTokens && targetInTokens) {
				return 0;
			}
			return super.getEdgeWeight(e);
		}

		private boolean isInTokens(Element edgeNode, List<Element> tokenizationElements, Set<Entity> entities) {
			if (edgeNode.isElementType(ElementType.ENTITY)) {
				return entities.contains(edgeNode);
			} else {
				return tokenizationElements.contains(edgeNode);
			}
		}

	}

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

			Set<Element> elements = new HashSet<>();
			for (Token whToken : tokenization.getTokens(ElementType.WH_WORD)) {
				for (Token token : tokenization.getAttached(whToken)) {
					if (!token.isType(ElementType.WH_WORD)) {
						elements.add(token.getElement());
					}
				}
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

	protected Set<Entity> getConstraintEntities(Set<Token> constraints, Entity projectionEntity) {
		Set<Entity> constraintEntities = new HashSet<>();
		for (Token constrToken : constraints) {
			if (constrToken.getEntityElement() != projectionEntity) {
				constraintEntities.add(constrToken.getEntityElement());
			}
		}
		if (constraintEntities.size() == 0) {
			return Sets.newHashSet(projectionEntity);
		}
		return constraintEntities;
	}

	protected List<DefaultWeightedEdge> getShortestPath(Tokenization tokenization, Entity projEntity, Set<Entity> constraintEntities) {
		ElementGraph graph = new ElementGraph(this.schema.getGraph(), tokenization.getElements(), getEntityElements(tokenization));
		FloydWarshallShortestPaths<Element, DefaultWeightedEdge> shortestPaths = new FloydWarshallShortestPaths<>(graph);
		List<Entity> bestOrdering = getBestConstraintEntitiesOrdering(graph, shortestPaths, projEntity, constraintEntities);
		System.out.println("ORDER: \n " + bestOrdering);
		List<DefaultWeightedEdge> shortestPath = getPathThroughOrdering(shortestPaths, projEntity, bestOrdering);
		System.out.println("PATH: \n " + shortestPath);
		return shortestPath;

	}

	private List<Entity> getBestConstraintEntitiesOrdering(DirectedWeightedMultigraph<Element, DefaultWeightedEdge> graph,
			FloydWarshallShortestPaths<Element, DefaultWeightedEdge> shortestPaths, Entity projEntity, Set<Entity> constraintEntities) {
		Collection<List<Entity>> constraintOrderings = Collections2.permutations(constraintEntities);
		double bestCost = Double.MAX_VALUE;
		List<Entity> bestOrdering = null;
		for (List<Entity> ordering : constraintOrderings) {
			double cost = 0;
			ImmutableList<Entity> orderingWithProj = ImmutableList.<Entity> builder().addAll(ordering).add(projEntity).build();
			Iterator<Entity> iterator = orderingWithProj.iterator();
			Entity source = iterator.next();
			while (iterator.hasNext()) {
				Entity target = iterator.next();
				if (source.equals(target)) {
					continue;
				}
				cost += shortestPaths.getShortestPath(source, target).getWeight();
				source = target;
			}
			if (cost < bestCost) {
				bestCost = cost;
				bestOrdering = ordering;
			}
		}
		return bestOrdering;
	}

	private List<DefaultWeightedEdge> getPathThroughOrdering(FloydWarshallShortestPaths<Element, DefaultWeightedEdge> shortestPaths,
			Entity projEntity, List<Entity> ordering) {
		List<DefaultWeightedEdge> shortestPath = new ArrayList<>();
		ImmutableList<Entity> orderingWithProj = ImmutableList.<Entity> builder().addAll(ordering).add(projEntity).build();
		Iterator<Entity> iterator = orderingWithProj.iterator();
		Entity source = iterator.next();
		while (iterator.hasNext()) {
			Entity target = iterator.next();
			if (source.equals(target)) {
				continue;
			}
			shortestPath.addAll(shortestPaths.getShortestPath(source, target).getEdgeList());
			source = target;
		}
		return shortestPath;
	}
}
