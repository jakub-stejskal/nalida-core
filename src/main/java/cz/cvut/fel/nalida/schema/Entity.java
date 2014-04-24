package cz.cvut.fel.nalida.schema;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = { "resource", "tokens", "attributes", "subresources" })
public class Entity extends Element {
	private String resource;
	private List<String> tokens = Collections.emptyList();
	private List<Attribute> attributes = Collections.emptyList();
	private List<Subresource> subresources = Collections.emptyList();

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

	public void setTokens(List<String> tokens) {
		this.tokens = tokens;
	}

	@XmlElementWrapper(name = "tokens")
	@XmlElement(name = "token")
	public List<String> getTokens() {
		return this.tokens;
	}

	public void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}

	@XmlElementWrapper(name = "attributes")
	@XmlElement(name = "attribute")
	public List<Attribute> getAttributes() {
		return this.attributes;
	}

	@XmlElementWrapper(name = "subresources")
	@XmlElement(name = "subresource")
	public void setSubresources(List<Subresource> subresources) {
		this.subresources = subresources;
	}

	public List<Subresource> getSubresources() {
		return this.subresources;
	}

	public String toStringDeep() {
		return this.name + "\n " + this.resource + "\n Attributes:" + this.attributes + ",\n Subresources:" + this.subresources;
	}
}
