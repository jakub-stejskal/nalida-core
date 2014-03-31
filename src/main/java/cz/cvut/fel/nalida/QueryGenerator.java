package cz.cvut.fel.nalida;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.google.common.collect.Iterables;

import cz.cvut.fel.nalida.db.Attribute;
import cz.cvut.fel.nalida.db.Element;
import cz.cvut.fel.nalida.db.Element.ElementType;
import cz.cvut.fel.nalida.db.Entity;
import cz.cvut.fel.nalida.db.QueryBuilder;
import cz.cvut.fel.nalida.db.QueryPlan;
import cz.cvut.fel.nalida.db.Schema;

public class QueryGenerator {

	protected final Schema schema;
	private final Properties props;

	public QueryGenerator(Schema schema, Properties props) throws FileNotFoundException, IOException {
		this.schema = schema;
		this.props = props;
	}

	public QueryPlan generateQuery(Tokenization tokenization) {
		QueryPlan queryPlan = new QueryPlan();

		Set<Element> projections = getProjectionElements(tokenization);
		Set<Token> constraints = getConstraintElements(tokenization);

		Set<Entity> resultEntities = new HashSet<>();
		for (Element projElement : projections) {
			resultEntities.add(projElement.toEntityElement());
		}
		if (resultEntities.size() > 1) {
			throw new UnsupportedOperationException("Multi-entity projections not supported.");
		}

		Entity resultEntity = getResource(resultEntities);

		Set<Entity> constraintEntities = new HashSet<>();
		Set<Token> constraintTokens = new HashSet<>();
		for (Token constrToken : constraints) {
			if (constrToken.getEntityElement() != resultEntity) {
				constraintTokens.add(constrToken);
				constraintEntities.add(constrToken.getEntityElement());
			}
		}
		if (constraintEntities.size() > 1) {
			throw new UnsupportedOperationException("Multiple non-result constraint entities not supported.");
		}

		else if (constraintEntities.size() == 1) {
			Entity constraintEntity = constraintEntities.iterator().next();
			QueryBuilder qb = new QueryBuilder(this.props);
			qb.resource(constraintEntity.getResource());

			//			for (Element projElement : projections) {
			//				qb.projection(getProjectionLabel(projElement, resultEntity));
			//			}

			for (Token token : constraintTokens) {
				String attribute = getResourceConstraintLabel(constraintEntity, token.getElement());
				qb.constraint(attribute, "==", Iterables.toArray(token.getWords(), String.class));
			}

			queryPlan.addQuery(qb.build());
		}

		QueryBuilder qb = new QueryBuilder(this.props);
		qb.resource(resultEntity.getResource());

		for (Element projElement : projections) {
			qb.projection(getProjectionLabel(projElement, resultEntity));
		}

		for (Token token : constraints) {
			if (token.getEntityElement() != constraintEntities.iterator().next()) {
				String attribute = getResourceConstraintLabel(resultEntity, token.getElement());
				qb.constraint(attribute, "==", Iterables.toArray(token.getWords(), String.class));
			}
		}

		queryPlan.addQuery(qb.build());
		return queryPlan;
	}

	protected Set<Element> getProjectionElements(Tokenization tokenization) {
		Token whToken = tokenization.getTokens(ElementType.WH_WORD).iterator().next();
		Set<Element> elements = new HashSet<>();
		for (Token token : tokenization.getAttached(whToken)) {
			elements.add(token.getElement());
		}
		return elements;
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

	private String getResourceConstraintLabel(Entity resultEntity, Element constraintElement) {
		Entity constraintEntity = constraintElement.toEntityElement();
		if (constraintEntity.equals(resultEntity)) {
			return constraintElement.getName();
		}
		for (Attribute resourceAttribute : resultEntity.getAttributes()) {
			if (resourceAttribute.getType().equals(constraintEntity.getName())) {
				return resourceAttribute.getName() + "." + constraintElement.getName();
			}
		}

		throw new UnsupportedOperationException("Result entity " + resultEntity.getName() + " and constraint entity "
				+ constraintEntity.getName() + " not connected.");
	}

	private String getProjectionLabel(Element element, Entity resourceEntity) {
		if (element.equals(resourceEntity)) {
			return null;
		}
		if (element.isElementType(ElementType.ATTRIBUTE) && element.toEntityElement().equals(resourceEntity)) {
			return "content/" + element.getName();
		}
		return "content/" + element.getName(); // TODO deal with non-resource projections
	}

	private Entity getResource(Set<Entity> entities) {
		return entities.iterator().next();
	}
}
