package cz.cvut.fel.nalida.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class QueryBuilder {
	private static final String PARAM_START = "?";
	private static final String PARAM_DELIM = "&";
	private final WebResource webResource;
	protected String resource;
	protected Set<String> projection;
	protected Map<String, String> constraints;
	boolean isCollection = true;

	public QueryBuilder(Properties properties) {

		String baseUrl = properties.getProperty("baseUrl");
		String user = properties.getProperty("auth.user");
		String password = properties.getProperty("auth.password");

		Client client = Client.create();
		client.addFilter(new HTTPBasicAuthFilter(user, password));

		this.webResource = client.resource(baseUrl);

		this.projection = new HashSet<>();
		this.constraints = new HashMap<>();
	}

	public QueryBuilder collection(boolean isColleciton) {
		this.isCollection = isColleciton;
		return this;
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

	public Query build() {
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();

		String proj = projectionParam();
		if (proj != null)
			params.add("fields", proj);
		String constr = constraintsParam();
		if (constr != null)
			params.add("query", constr);
		params.putAll(defaultParams());

		return new Query(this.webResource.path(this.resource).queryParams(params));
	}

	@Override
	public String toString() {
		List<String> params = new ArrayList<>();
		params.add(projectionParam());
		params.add(constraintsParam());
		return this.resource + "\n" + PARAM_START + Joiner.on("\n" + PARAM_DELIM).skipNulls().join(params);

	}

	private Map<String, List<String>> defaultParams() {
		Map<String, List<String>> m = new HashMap<>();
		m.put("lang", Lists.newArrayList("en"));
		m.put("multilang", Lists.newArrayList("false"));
		m.put("limit", Lists.newArrayList("100"));
		m.put("sem", Lists.newArrayList("current", "next"));
		return m;
	}

	private String projectionParam() {
		if (this.projection.isEmpty()) {
			return null;
		}
		String projections = Joiner.on(",").skipNulls().join(this.projection);
		return this.isCollection ? "id,link,entry(" + projections + ")" : projections;
	}

	private String constraintsParam() {
		if (this.constraints.isEmpty()) {
			return null;
		}
		return Joiner.on(";").withKeyValueSeparator("").join(this.constraints);
	}
}
