package cz.cvut.fel.nalida.db;

abstract public class Element {

	public static Element WH_ELEMENT = new Element() {
		@Override
		public ElementType getElementType() {
			return ElementType.WH_WORD;
		}

		@Override
		public Entity toEntityElement() {
			throw new UnsupportedOperationException("WhElement cannot be converted to EntityElement");
		}

		@Override
		public Entity getParent() {
			throw new UnsupportedOperationException("WhElement does not have parent");
		}

		@Override
		public String toString() {
			return getElementType().toString();
		};
	};

	public enum ElementType {
		ATTRIBUTE, ENTITY, VALUE, WH_WORD, SUBRESOURCE;
	}

	protected String name;

	public Element() {
	}

	abstract public ElementType getElementType();

	abstract public Entity toEntityElement();

	abstract protected Element getParent();

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public String getLongName() {
		return this.name;
	}

	@Override
	public String toString() {
		return getElementType() + "/" + getLongName();
	}

	public boolean isElementType(ElementType... types) {
		for (ElementType type : types) {
			if (getElementType() == type) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object that) {
		if (this == that)
			return true;

		if (!(that instanceof Element))
			return false;

		Element thatElement = (Element) that;
		return this.toString().equals(thatElement.toString());
	}
}
