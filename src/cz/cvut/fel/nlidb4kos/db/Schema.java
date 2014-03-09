package cz.cvut.fel.nlidb4kos.db;

import static java.lang.String.format;

import java.util.Map;

public final class Schema {
	private String version;
	private Map<String, Entity> schema;

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Map<String, Entity> getSchema() {
		return schema;
	}

	public void setSchema(Map<String, Entity> users) {
		this.schema = users;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(format("Version: %s\n", version)).append(format("mySchema: %s\n", schema)).toString();
	}
}
