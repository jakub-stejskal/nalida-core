package cz.cvut.fel.nalida;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

import cz.cvut.fel.nalida.db.Element;
import cz.cvut.fel.nalida.db.Lexicon.ElementType;
import cz.cvut.fel.nalida.db.Query;
import cz.cvut.fel.nalida.db.QueryBuilder;
import cz.cvut.fel.nalida.db.Schema;

public class QueryGenerator {

	@SuppressWarnings("unused")
	private final Schema schema;
	private final Properties props;

	public QueryGenerator(Schema schema, Properties props) throws FileNotFoundException, IOException {
		this.schema = schema;
		this.props = props;
	}

	public Query generateQuery(Tokenization tokenization) {
		Set<Element> projections = getProjectionElements(tokenization);
		Set<Element> entities = getEntityElements(tokenization);
		Set<Token> constraints = getConstraintElements(tokenization);

		return createQueryPlan(projections, entities, constraints);
	}

	private Query createQueryPlan(Set<Element> projections, Set<Element> entities, Set<Token> constraints) {
		return fillQuery(projections, entities, constraints).build();
	}

	private Set<Element> getProjectionElements(Tokenization tokenization) {
		Token whToken = tokenization.getTokens(ElementType.WH_WORD).iterator().next();
		Set<Element> elements = new HashSet<>();
		for (Token token : tokenization.getAttached(whToken)) {
			elements.add(token.getElement());
		}
		return elements;
	}

	private Set<Element> getEntityElements(Tokenization tokenization) {
		Set<Element> entities = new HashSet<>();
		for (Token token : tokenization.getTokens()) {
			if (token.isType(ElementType.ENTITY)) {
				entities.add(token.getEntityElement());
			} else if (token.isType(ElementType.ATTRIBUTE, ElementType.VALUE)) {
				entities.add(token.getEntityElement());
			}
		}
		return entities;
	}

	private Set<Token> getConstraintElements(Tokenization tokenization) {
		return new HashSet<>(tokenization.getTokens(ElementType.VALUE));
	}

	private QueryBuilder fillQuery(Set<Element> projections, Set<Element> entities, Set<Token> constraints) {
		QueryBuilder qb = new QueryBuilder(this.props);

		qb.projection("title", "link");
		for (Element token : projections) {
			qb.projection(getProjectionLabel(token, getResourceName(entities)));
		}

		Set<String> entityNames = new HashSet<>();
		for (Element element : entities) {
			entityNames.add(element.getElementName().toLowerCase() + "s"); // TODO Resource selection, Token->Resource
		}
		qb.resource(Joiner.on("-").join(entityNames));

		for (Token token : constraints) {
			qb.constraint(getConstraintLabel(token, getResourceName(entities)), "==", Iterables.toArray(token.getWords(), String.class));
		}
		return qb;
	}

	private String getProjectionLabel(Element element, String resourceName) {
		if (element.getElementType() == ElementType.ENTITY && element.getElementName().equals(resourceName)) {
			return null;
		}
		if (element.getElementName().startsWith(resourceName + ".")) {
			return "content/" + element.getElementName().replaceFirst(resourceName + ".", "");
		}
		return "content/" + element.getElementName();
	}

	private String getConstraintLabel(Token token, String resourceName) {
		if (token.getElementName().startsWith(resourceName + ".")) {
			return token.getElementName().replaceFirst(resourceName + ".", "");
		}
		return token.getElementName();
	}

	private String getResourceName(Set<Element> entities) {
		return entities.iterator().next().getElementName();
	}
}
