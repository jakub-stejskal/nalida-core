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

	public Object getPath(Element from, Element to) {
		return null;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(format("Version: %s\n", this.version)).append(format("mySchema: %s\n", this.schema)).toString();
	}
}
