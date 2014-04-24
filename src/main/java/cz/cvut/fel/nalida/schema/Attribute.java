package cz.cvut.fel.nalida.schema;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;

public class Attribute extends Element {
	private String type;
	protected Entity typeEntity;
	private List<String> tokens = Collections.emptyList();
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

	@XmlTransient
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

	@XmlElementWrapper(name = "tokens")
	@XmlElement(name = "token")
	public List<String> getTokens() {
		return this.tokens;
	}
}
