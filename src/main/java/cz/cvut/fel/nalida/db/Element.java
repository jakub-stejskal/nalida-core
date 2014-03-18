package cz.cvut.fel.nalida.db;

import cz.cvut.fel.nalida.db.Lexicon.ElementType;

public class Element {

	protected Element parent;
	protected ElementType elementType;
	protected String elementName;

	public Element(ElementType elementType, String elementName) {
		this.elementType = elementType;
		this.elementName = elementName;
	}

	public ElementType getElementType() {
		return this.elementType;
	}

	public String getElementName() {
		return this.elementName;
	}

	@Override
	public String toString() {
		return this.elementType + "/" + this.elementName;
	}

	public boolean isType(ElementType... types) {
		for (ElementType type : types) {
			if (this.elementType == type) {
				return true;
			}
		}
		return false;
	}

	public Element toEntityElement() {
		switch (this.elementType) {
		case ENTITY:
			return this;
		case ATTRIBUTE:
			return this.parent;
		case VALUE:
			return this.parent.parent;
		case WH_WORD:
			throw new UnsupportedOperationException("Wh-word cannot be converted to Entity");
		default:
			throw new UnsupportedOperationException("Unknown element type.");
		}
	}

	@Override
	public int hashCode() {
		return this.elementName.hashCode() + this.elementType.hashCode();
	}

	@Override
	public boolean equals(Object that) {
		if (this == that)
			return true;

		if (!(that instanceof Element))
			return false;

		Element thatToken = (Element) that;

		return this.elementName.equals(thatToken.elementName) && this.elementType.equals(thatToken.elementType);
	}
}
