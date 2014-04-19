package cz.cvut.fel.nalida.schema;

public class Subresource extends Attribute {
	@Override
	public ElementType getElementType() {
		return ElementType.SUBRESOURCE;
	}

	@Override
	public String getLongName() {
		return getParent().getName() + "." + getName();
	}
}
