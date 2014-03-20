package cz.cvut.fel.nalida;

import java.util.Set;

import cz.cvut.fel.nalida.db.Element;
import cz.cvut.fel.nalida.db.Element.ElementType;
import cz.cvut.fel.nalida.db.Entity;

public class Token {
	protected Set<String> words;
	protected Element element;
	protected Set<Element> elements;

	//
	//	public Token(ElementType elementType, String elementName) {
	//		this(null, elementType, elementName);
	//	}
	//
	//	public Token(Set<String> words, Token token) {
	//		this(words, token.elementType, token.elementName);
	//	}
	//
	public Token(Set<String> words, Element element) {
		super();
		this.words = words;
		this.element = element;
	}

	public Set<String> getWords() {
		return this.words;
	}

	public ElementType getElementType() {
		return this.element.getElementType();
	}

	public String getElementName() {
		return this.element.getName();
	}

	@Override
	public String toString() {
		return this.words + "/" + this.element;
	}

	public boolean isType(ElementType... types) {
		for (ElementType type : types) {
			if (this.element.getElementType() == type) {
				return true;
			}
		}
		return false;
	}

	public Entity getEntityElement() {
		return this.element.toEntityElement();
	}

	public Element getElement() {
		return this.element;
	}

	@Override
	public boolean equals(Object that) {
		if (this == that)
			return true;

		if (!(that instanceof Token))
			return false;

		Token thatToken = (Token) that;

		return this.words.equals(thatToken.words) && this.element.equals(thatToken.element);
	}
}
