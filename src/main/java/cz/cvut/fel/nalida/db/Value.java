package cz.cvut.fel.nalida.db;

public class Value extends Element {
	protected Attribute parent;

	public Value(Attribute parent) {
		this.parent = parent;
	}

	@Override
	public ElementType getElementType() {
		return ElementType.VALUE;
	}

	@Override
	public Entity toEntityElement() {
		return getParent().getParent();
	}

	@Override
	protected Attribute getParent() {
		return this.parent;
	}

	@Override
	public String getName() {
		return this.parent.getName();
	}

	@Override
	public String toString() {
		return getElementType() + "/" + toEntityElement().getName() + "." + getName();
	}
}