package cz.cvut.fel.nlidb4kos.db;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class QueryBuilder {
	private static final String PARAM_START = "?";
	private static final String PARAM_DELIM = "&";
	protected URL baseUrl;
	protected String resource;
	protected Set<String> projection;
	protected Map<String, String> constraints;

	public QueryBuilder(URL baseURL) {
		this.baseUrl = baseURL;
		this.projection = new HashSet<>();
		this.constraints = new HashMap<>();
	}

	public QueryBuilder resource(String resource) {
		this.resource = resource;
		return this;
	}

	public QueryBuilder projection(String... attributes) {
		projection.addAll(Arrays.asList(attributes));
		return this;
	}

	public QueryBuilder constraint(String attribute, String operator, String value) {
		constraints.put(attribute, operator + value);
		return this;
	}

	public String build() {
		return baseUrl + resource + PARAM_START + constraintsToString() + PARAM_DELIM + projectionToString();
	}

	private String projectionToString() {
		StringBuilder sb = new StringBuilder();
		sb.append("fields=");
		for (String p : projection) {
			sb.append(p);
			sb.append(",");
		}
		return sb.toString();
	}

	private String constraintsToString() {
		StringBuilder sb = new StringBuilder();
		sb.append("query=");
		for (Entry<String, String> c : constraints.entrySet()) {
			sb.append(c.getKey() + c.getValue());
			sb.append(";");
		}
		return sb.toString();
	}
}
