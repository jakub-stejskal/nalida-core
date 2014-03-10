package cz.cvut.fel.nlidb4kos;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

import cz.cvut.fel.nlidb4kos.db.Lexicon.ElementType;
import cz.cvut.fel.nlidb4kos.db.QueryBuilder;
import cz.cvut.fel.nlidb4kos.db.Schema;

public class QueryGenerator {

	private final Schema schema;
	private final URL baseUrl;

	public QueryGenerator(Schema schema, URL baseUrl) throws FileNotFoundException, IOException {
		this.schema = schema;
		this.baseUrl = baseUrl;
	}

	String generateQuery(Tokenization tokenization) {
		Set<Token> projections = getProjectionElements(tokenization);
		Set<Token> entities = getEntityElements(tokenization);
		Set<Token> constraints = getConstraintElements(tokenization);
		return createQuery(projections, entities, constraints);
	}

	private Set<Token> getProjectionElements(Tokenization tokenization) {
		Token whToken = tokenization.getTokens(ElementType.WH_WORD).iterator().next();
		return tokenization.getAttached(whToken);
	}

	private Set<Token> getEntityElements(Tokenization tokenization) {
		Set<Token> entities = new HashSet<>();
		for (Token token : tokenization.getTokens()) {
			if (token.isType(ElementType.ENTITY)) {
				entities.add(token);
			} else if (token.isType(ElementType.ATTRIBUTE, ElementType.VALUE)) {
				entities.add(token.toEntityToken());
			}
		}
		return entities;
	}

	private Set<Token> getConstraintElements(Tokenization tokenization) {
		return new HashSet<>(tokenization.getTokens(ElementType.VALUE));
	}

	private String createQuery(Set<Token> projections, Set<Token> entities, Set<Token> constraints) {
		QueryBuilder qb = new QueryBuilder(this.baseUrl);

		qb.projection("title");
		for (Token token : projections) {
			qb.projection(getProjectionLabel(token, getResourceName(entities)));
		}

		Set<String> entityNames = new HashSet<>();
		for (Token token : entities) {
			entityNames.add(token.getEntityName() + "s"); // TODO Resource selection, Token->Resource
		}
		qb.resource(Joiner.on("-").join(entityNames));

		for (Token token : constraints) {
			qb.constraint(getConstraintLabel(token, getResourceName(entities)), "==", Iterables.toArray(token.getWords(), String.class));
		}
		return qb.build();
	}

	private String getProjectionLabel(Token token, String resourceName) {
		if (token.getEntityType() == ElementType.ENTITY && token.getEntityName().equals(resourceName)) {
			return null;
		}
		if (token.getEntityName().startsWith(resourceName + ".")) {
			return "content/" + token.getEntityName().replaceFirst(resourceName + ".", "");
		}
		return "content/" + token.getEntityName();
	}

	private String getConstraintLabel(Token token, String resourceName) {
		if (token.getEntityName().startsWith(resourceName + ".")) {
			return token.getEntityName().replaceFirst(resourceName + ".", "");
		}
		return token.getEntityName();
	}

	private String getResourceName(Set<Token> entities) {
		return entities.iterator().next().getEntityName();
	}
}
