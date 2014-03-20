package cz.cvut.fel.nalida;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

import cz.cvut.fel.nalida.db.Attribute;
import cz.cvut.fel.nalida.db.Element;
import cz.cvut.fel.nalida.db.Element.ElementType;
import cz.cvut.fel.nalida.db.Entity;
import cz.cvut.fel.nalida.db.Query;
import cz.cvut.fel.nalida.db.QueryBuilder;
import cz.cvut.fel.nalida.db.Schema;

public class QueryGenerator {

	@SuppressWarnings("unused")
	// TODO Will it be used here?
	private final Schema schema;
	private final Properties props;

	public QueryGenerator(Schema schema, Properties props) throws FileNotFoundException, IOException {
		this.schema = schema;
		this.props = props;
	}

	public Query generateQuery(Tokenization tokenization) {
		Set<Element> projections = getProjectionElements(tokenization);
		Set<Entity> entities = getEntityElements(tokenization);
		Set<Token> constraints = getConstraintElements(tokenization);

		return createQueryPlan(projections, entities, constraints);
	}

	private Set<Element> getProjectionElements(Tokenization tokenization) {
		Token whToken = tokenization.getTokens(ElementType.WH_WORD).iterator().next();
		Set<Element> elements = new HashSet<>();
		for (Token token : tokenization.getAttached(whToken)) {
			elements.add(token.getElement());
		}
		return elements;
	}

	private Set<Entity> getEntityElements(Tokenization tokenization) {
		Set<Entity> entities = new HashSet<>();
		for (Token token : tokenization.getTokens()) {
			if (!token.isType(ElementType.WH_WORD)) {
				entities.add(token.getEntityElement());
			}
		}
		return entities;
	}

	private Set<Token> getConstraintElements(Tokenization tokenization) {
		return new HashSet<>(tokenization.getTokens(ElementType.VALUE));
	}

	private Query createQueryPlan(Set<Element> projections, Set<Entity> entities, Set<Token> constraints) {
		if (entities.size() == 1) {
			return fillQuery(projections, entities, constraints).build();
		}

		Set<Entity> projEntities = new HashSet<>();
		for (Element element : projections) {
			projEntities.add(element.toEntityElement());
		}

		if (projEntities.size() == 1) {
			QueryBuilder qb = new QueryBuilder(this.props);
			Entity resourceEntity = projEntities.iterator().next();

			qb.resource(resourceEntity.getResource());

			qb.projection("title", "link");
			for (Element projElement : projections) {
				qb.projection(getProjectionLabel(projElement, resourceEntity));
			}

			for (Token token : constraints) {
				String attribute = getResourceConstraintLabel(resourceEntity, token.getElement());
				qb.constraint(attribute, "==", Iterables.toArray(token.getWords(), String.class));

			}
			return qb.build();
		}

		throw new UnsupportedOperationException("Multi-entity projections not supported.");
	}

	private String getResourceConstraintLabel(Entity resourceEntity, Element constraintElement) {
		Entity constraintEntity = constraintElement.toEntityElement();
		if (constraintEntity.equals(resourceEntity)) {
			return constraintElement.getName();
		} else {
			for (Attribute attribute : resourceEntity.getAttributes()) {
				if (attribute.getType().equals(constraintEntity.getName())) {
					return attribute.getName() + "." + constraintElement.getName();
				}
			}
		}
		throw new UnsupportedOperationException("Resource entity " + resourceEntity.getName() + " and constraint entity " + constraintEntity.getName()
				+ " not connected.");
	}

	private QueryBuilder fillQuery(Set<Element> projections, Set<Entity> entities, Set<Token> constraints) {
		QueryBuilder qb = new QueryBuilder(this.props);

		qb.projection("title", "link");
		for (Element token : projections) {
			qb.projection(getProjectionLabel(token, getResource(entities)));
		}

		Set<String> entityNames = new HashSet<>();
		for (Entity entity : entities) {
			entityNames.add(entity.getResource());
		}
		qb.resource(Joiner.on("-").join(entityNames)); // TODO Resource selection, Token->Resource

		for (Token token : constraints) {
			qb.constraint(getConstraintLabel(token, getResource(entities)), "==", Iterables.toArray(token.getWords(), String.class));
		}
		return qb;
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

	private String getConstraintLabel(Token token, Entity resourceEntity) {
		if (token.getElementName().startsWith(resourceEntity + ".")) {
			return token.getElementName().replaceFirst(resourceEntity + ".", "");
		}
		return token.getElementName();
	}

	private Entity getResource(Set<Entity> entities) {
		return entities.iterator().next();
	}
}
