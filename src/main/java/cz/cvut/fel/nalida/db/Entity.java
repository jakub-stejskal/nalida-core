package cz.cvut.fel.nalida.db;

import java.util.Collections;
import java.util.List;

public class Entity extends Element {
	private String resource;
	private List<Attribute> attributes = Collections.emptyList();
	private List<Attribute> subresources = Collections.emptyList();

	@Override
	public ElementType getElementType() {
		return ElementType.ENTITY;
	}

	@Override
	public Entity toEntityElement() {
		return this;
	}

	@Override
	protected Element getParent() {
		throw new UnsupportedOperationException("Entity does not have parent");
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getResource() {
		return this.resource;
	}

	public void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}

	public List<Attribute> getAttributes() {
		return this.attributes;
	}

	public void setSubresources(List<Attribute> subresources) {
		this.subresources = subresources;
	}

	public List<Attribute> getSubresources() {
		return this.subresources;
	}

	public String toStringDeep() {
		return this.name + "\n " + this.resource + "\n Attributes:" + this.attributes + ",\n Subresources:" + this.subresources;
	}
}
