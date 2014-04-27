package cz.cvut.fel.nalida.interpretation;

import java.util.List;

import cz.cvut.fel.nalida.schema.Element;
import cz.cvut.fel.nalida.schema.Entity;
import cz.cvut.fel.nalida.schema.Element.ElementType;

public class Token {
	protected List<String> words;
	protected Element element;

	public Token(List<String> words, Element element) {
		super();
		this.words = words;
		this.element = element;
	}

	public List<String> getWords() {
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
	public int hashCode() {
		return this.words.hashCode() + this.element.hashCode();
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
