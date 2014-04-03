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

	public Attribute getAttribute() {
		return getParent();
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
	public String getLongName() {
		return toEntityElement().getName() + "." + getName();
	}
}
