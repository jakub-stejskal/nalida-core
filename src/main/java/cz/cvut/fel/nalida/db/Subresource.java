package cz.cvut.fel.nalida.db;

public class Subresource extends Attribute {
	@Override
	public ElementType getElementType() {
		return ElementType.SUBRESOURCE;
	}

	@Override
	public Entity toEntityElement() {
		return getTypeEntity();
	}

	@Override
	public String getLongName() {
		return getParent().getName() + "." + getName();
	}
}
