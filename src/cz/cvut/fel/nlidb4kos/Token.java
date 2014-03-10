package cz.cvut.fel.nlidb4kos;

import java.util.Set;

import cz.cvut.fel.nlidb4kos.db.Lexicon.ElementType;

public class Token {
	protected Set<String> words;
	protected ElementType entityType;
	protected String entityName;

	public Token(ElementType entityType, String entityName) {
		this(null, entityType, entityName);
	}

	public Token(Set<String> words, Token token) {
		this(words, token.entityType, token.entityName);
	}

	public Token(Set<String> words, ElementType entityType, String entityName) {
		super();
		this.words = words;
		this.entityType = entityType;
		this.entityName = entityName;
	}

	public Set<String> getWords() {
		return this.words;
	}

	public ElementType getEntityType() {
		return this.entityType;
	}

	public String getEntityName() {
		return this.entityName;
	}

	@Override
	public String toString() {
		return this.words + "/" + this.entityType + "/" + this.entityName;
	}

	public boolean isType(ElementType... types) {
		for (ElementType type : types) {
			if (this.entityType == type) {
				return true;
			}
		}
		return false;
	}

	public Token toEntityToken() {
		try {
			return new Token(this.words, ElementType.ENTITY, this.entityName.split("\\.")[0]);
		} catch (Exception e) {
			throw new RuntimeException(this.entityName, e);
		}
	}

	@Override
	public boolean equals(Object that) {
		if (this == that)
			return true;

		if (!(that instanceof Token))
			return false;

		Token thatToken = (Token) that;

		return this.words.equals(thatToken.words) && this.entityType.equals(thatToken.entityType) && this.entityName.equals(thatToken.entityName);
	}
}
