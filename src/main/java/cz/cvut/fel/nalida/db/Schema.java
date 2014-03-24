package cz.cvut.fel.nalida.db;

import static java.lang.String.format;

import java.util.List;

public final class Schema {
	private String version;
	private List<Entity> schema;

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public List<Entity> getSchema() {
		return this.schema;
	}

	public void setSchema(List<Entity> schema) {
		this.schema = schema;
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		sb.append(format("Version: %s\n", this.version));
		sb.append(format("Schema: \n"));

		for (Entity entity : this.schema) {
			sb.append(entity.toStringDeep() + "\n");
		}

		return sb.toString();
	}

	public Schema linkReferences() {
		for (Entity entity : this.schema) {
			for (Attribute attribute : entity.getAttributes()) {
				attribute.parent = entity;
			}
			for (Attribute attribute : entity.getSubresources()) {
				attribute.parent = entity;
			}
		}
		return this;
	}
}
