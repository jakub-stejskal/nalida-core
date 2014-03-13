package cz.cvut.fel.nalida.db;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

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
		this.projection.addAll(Arrays.asList(attributes));
		return this;
	}

	public QueryBuilder constraint(String attribute, String operator, String... values) {
		String concatValue = "*" + Joiner.on("*").join(values) + "*";
		this.constraints.put(attribute, operator + concatValue.toString());
		return this;
	}

	public String build() {
		List<String> params = new ArrayList<>();
		params.add(projectionParam());
		params.add(constraintsParam());
		params.addAll(defaultParams());

		return this.baseUrl + this.resource + PARAM_START + Joiner.on(PARAM_DELIM).skipNulls().join(params);
	}

	@Override
	public String toString() {
		List<String> params = new ArrayList<>();
		params.add(projectionParam());
		params.add(constraintsParam());
		return "/" + this.resource + "\n" + PARAM_START + Joiner.on("\n" + PARAM_DELIM).skipNulls().join(params);

	}

	private List<String> defaultParams() {
		return Lists.newArrayList("lang=en", "multilang=false", "limit=100", "sem=current,next");
	}

	private String projectionParam() {
		if (this.projection.isEmpty()) {
			return null;
		}
		return "fields=entry(" + Joiner.on(",").skipNulls().join(this.projection) + ")";
	}

	private String constraintsParam() {
		if (this.constraints.isEmpty()) {
			return null;
		}
		return "query=" + Joiner.on(";").withKeyValueSeparator("").join(this.constraints);
	}
}
