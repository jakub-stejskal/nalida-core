package cz.cvut.fel.nalida.db;

import java.util.List;

public class Attribute extends Element {
	private String type;
	protected Entity typeEntity;
	private List<String> tokens;
	private final Value valueElement;
	protected Entity parent;

	public Attribute() {
		this.valueElement = new Value(this);
	}

	@Override
	public ElementType getElementType() {
		return ElementType.ATTRIBUTE;
	}

	@Override
	public String getLongName() {
		return toEntityElement().getName() + "." + getName();
	}

	public Entity getTypeEntity() {
		return this.typeEntity;
	}

	@Override
	public Entity toEntityElement() {
		return getParent();
	}

	@Override
	protected Entity getParent() {
		return this.parent;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setTypeEntity(Entity typeEntity) {
		this.typeEntity = typeEntity;
	}

	public String getType() {
		return this.type;
	}

	public boolean isPrimitiveType() {
		return this.type.equals("string") || this.type.equals("integer");
	}

	public boolean isCollectionType() {
		return this.type.endsWith("*");
	}

	public Value getValueElement() {
		return this.valueElement;
	}

	public void setTokens(List<String> tokens) {
		this.tokens = tokens;
	}

	public List<String> getTokens() {
		return this.tokens;
	}
}
