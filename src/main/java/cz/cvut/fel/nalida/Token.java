package cz.cvut.fel.nalida;

import java.util.Set;

import cz.cvut.fel.nalida.db.Element;
import cz.cvut.fel.nalida.db.Lexicon.ElementType;

public class Token {
	protected Set<String> words;
	protected ElementType elementType;
	protected String elementName;

	public Token(ElementType elementType, String elementName) {
		this(null, elementType, elementName);
	}

	public Token(Set<String> words, Token token) {
		this(words, token.elementType, token.elementName);
	}

	public Token(Set<String> words, ElementType elementType, String elementName) {
		super();
		this.words = words;
		this.elementType = elementType;
		this.elementName = elementName;
	}

	public Set<String> getWords() {
		return this.words;
	}

	public ElementType getElementType() {
		return this.elementType;
	}

	public String getElementName() {
		return this.elementName;
	}

	@Override
	public String toString() {
		return this.words + "/" + this.elementType + "/" + this.elementName;
	}

	public boolean isType(ElementType... types) {
		for (ElementType type : types) {
			if (this.elementType == type) {
				return true;
			}
		}
		return false;
	}

	public Element getEntityElement() {
		try {
			if (this.elementType == ElementType.ENTITY) {
				return new Element(ElementType.ENTITY, this.elementName);
			}
			return new Element(ElementType.ENTITY, this.elementName.split("\\.")[0]);
		} catch (Exception e) {
			throw new RuntimeException(this.elementName, e);
		}
	}

	public Element getElement() {
		return new Element(ElementType.ENTITY, this.elementName);
	}

	@Override
	public boolean equals(Object that) {
		if (this == that)
			return true;

		if (!(that instanceof Token))
			return false;

		Token thatToken = (Token) that;

		return this.words.equals(thatToken.words) && this.elementType.equals(thatToken.elementType) && this.elementName.equals(thatToken.elementName);
	}
}
