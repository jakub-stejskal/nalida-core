package cz.cvut.fel.nalida.db;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.base.Joiner;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

public class QueryBuilder {
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

		this.resource = "";
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
		return new RestQuery(this.webResource, this.resource, this.projection, this.constraints, this.isCollection);
	}

	@Override
	public String toString() {
		return build().toString();
	}
}
