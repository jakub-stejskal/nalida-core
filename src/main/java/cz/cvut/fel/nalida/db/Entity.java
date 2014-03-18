package cz.cvut.fel.nalida.db;

import java.util.List;

public class Entity {
	private String name;
	private String resource;
	private List<Attribute> attributes;
	private List<Attribute> subresources;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
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

	@Override
	public String toString() {
		return "myAttr:" + this.attributes.toString() + ", mySubRes:" + this.attributes.toString();
	}
}
