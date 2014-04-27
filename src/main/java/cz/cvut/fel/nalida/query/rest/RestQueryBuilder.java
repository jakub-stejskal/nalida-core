package cz.cvut.fel.nalida.query.rest;

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

import cz.cvut.fel.nalida.query.Query;

public class RestQueryBuilder {
	private final WebResource webResource;
	protected String resource;
	protected Set<String> projection;
	protected Map<String, String> constraints;
	boolean isCollection = true;
	private int offset = 0;
	private int limit = 100;

	public RestQueryBuilder(Properties properties) {

		String baseUrl = properties.getProperty("baseUrl");
		String user = properties.getProperty("auth.user");
		String password = properties.getProperty("auth.password");

		Client client = Client.create();
		client.addFilter(new HTTPBasicAuthFilter(user, password));

		this.webResource = client.resource(baseUrl);

		this.resource = null;
		this.projection = new HashSet<>();
		this.constraints = new HashMap<>();
	}

	public RestQueryBuilder collection(boolean isColleciton) {
		this.isCollection = isColleciton;
		return this;
	}

	public RestQueryBuilder resource(String resource) {
		if (this.resource == null) {
			this.resource = resource;
		}
		return this;
	}

	public RestQueryBuilder projection(String... attributes) {
		this.projection.addAll(Arrays.asList(attributes));
		return this;
	}

	public RestQueryBuilder constraint(String attribute, String operator, String... values) {
		String concatValue = "*" + Joiner.on("*").join(values) + "*";
		this.constraints.put(attribute, operator + concatValue.toString());
		return this;
	}

	public RestQueryBuilder constraintExact(String attribute, String operator, String value) {
		this.constraints.put(attribute, operator + value);
		return this;
	}

	public RestQueryBuilder offset(int offset) {
		this.offset = offset;
		return this;
	}

	public RestQueryBuilder limit(int limit) {
		this.limit = limit;
		return this;
	}

	public Query build() {
		return new RestQuery(this.webResource, this.resource, this.projection, this.constraints, this.isCollection, this.offset, this.limit);
	}

	@Override
	public String toString() {
		return build().toString();
	}
}
