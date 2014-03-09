package cz.cvut.fel.nlidb4kos.db;

import java.util.List;

public class Entity {
	private List<Attribute> attributes;
	private List<Attribute> subresources;

	public void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}

	public List<Attribute> getAttributes() {
		return attributes;
	}

	public void setSubresources(List<Attribute> subresources) {
		this.subresources = subresources;
	}

	public List<Attribute> getSubresources() {
		return subresources;
	}

	@Override
	public String toString() {
		return "myAttr:" + attributes.toString() + ", mySubRes:" + attributes.toString();
	}
}
