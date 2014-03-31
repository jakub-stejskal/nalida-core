package cz.cvut.fel.nalida.db;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;
import javax.xml.xpath.XPathExpressionException;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class RestQuery implements Query {

	private final WebResource webResource;
	private final String resource;
	private final Set<String> projection;
	private final Map<String, String> constraints;
	private final boolean isCollection;

	public RestQuery(WebResource entryPoint, String resource, Set<String> projection, Map<String, String> constraints, boolean isCollection) {
		this.resource = resource;
		this.projection = projection;
		this.constraints = constraints;
		this.isCollection = isCollection;
		this.webResource = entryPoint.queryParams(params());
	}

	private MultivaluedMap<String, String> params() {
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();

		String proj = projectionParam();
		if (proj != null)
			params.add("fields", proj);
		String constr = constraintsParam();
		if (constr != null)
			params.add("query", constr);
		params.putAll(defaultParams());
		return params;
	}

	private Map<String, List<String>> defaultParams() {
		Map<String, List<String>> m = new HashMap<>();
		m.put("lang", Lists.newArrayList("en"));
		m.put("multilang", Lists.newArrayList("false"));
		m.put("limit", Lists.newArrayList("100"));
		m.put("sem", Lists.newArrayList("current,next"));
		return m;
	}

	private String projectionParam() {
		Set<String> projectionWithDefaults = new HashSet<>();
		projectionWithDefaults.add("title");
		projectionWithDefaults.add("link");
		projectionWithDefaults.addAll(this.projection);

		String projectionParam = Joiner.on(",").skipNulls().join(projectionWithDefaults);
		return this.isCollection ? "id,link,entry(" + projectionParam + ")" : projectionParam;
	}

	private String constraintsParam() {
		if (this.constraints.isEmpty()) {
			return null;
		}
		return Joiner.on(";").withKeyValueSeparator("").join(this.constraints);
	}

	@Override
	public String execute() throws Exception {
		return execute("");
	}

	@Override
	public List<String> execute(List<String> ids) throws Exception {
		List<String> responses = new ArrayList<>();
		if (ids.isEmpty()) {
			responses.add(execute(""));
		} else {
			for (String id : ids) {
				responses.add(execute(id));
			}
		}
		return responses;
	}

	private String execute(String id) throws Exception {
		WebResource request = this.webResource.path(id + this.resource);
		ClientResponse response = request.accept("application/xml").get(ClientResponse.class);
		System.out.println("Query.execute.request: " + request);
		if (response.getStatus() != 200) {
			System.out.println("Query.execute.response: " + response.getEntity(String.class));
			System.out.println();
			throw new Exception("Failed : HTTP error code : " + response.getStatus() + " - " + response.getStatusInfo() + ", URL:"
					+ this.webResource.getURI());
		}
		return response.getEntity(String.class);
	}

	@Override
	public List<String> project(List<String> queryResponse) throws XPathExpressionException {
		List<String> allResults = new ArrayList<>();
		for (String responseElement : queryResponse) {

			XmlParser coursesDoc = new XmlParser(responseElement);

			String query;
			if (this.projection.isEmpty()) {
				query = "//entry/link/@href";
			} else {
				String idAttr = this.projection.iterator().next();
				query = "//content/" + idAttr + "/@href";
			}

			List<String> results = coursesDoc.query(query);
			allResults.addAll(results);
		}
		return allResults;
	}

	@Override
	public String toString() {
		try {
			return URLDecoder.decode(this.webResource.path("<id>" + this.resource).toString(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return this.webResource.toString();
		}
	}
}
