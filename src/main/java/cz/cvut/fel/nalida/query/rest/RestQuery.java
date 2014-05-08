package cz.cvut.fel.nalida.query.rest;

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

import cz.cvut.fel.nalida.query.Query;
import cz.cvut.fel.nalida.util.XmlParser;

public class RestQuery implements Query {

	private final WebResource webResource;
	private final String resource;
	private final Set<String> projection;
	private final Map<String, String> constraints;
	private final boolean isCollection;
	private final int offset;
	private final int limit;

	public RestQuery(WebResource entryPoint, String resource, Set<String> projection, Map<String, String> constraints,
			boolean isCollection, int offset, int limit) {
		this.resource = resource;
		this.projection = projection;
		this.constraints = constraints;
		this.isCollection = isCollection;
		this.offset = offset;
		this.limit = limit;
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
		m.put("limit", Lists.newArrayList(String.valueOf(this.limit)));
		m.put("offset", Lists.newArrayList(String.valueOf(this.offset)));
		m.put("sem", Lists.newArrayList("current,next"));
		m.put("detail", Lists.newArrayList("1"));
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
		return execute("").toString();
	}

	@Override
	public String execute(Set<String> ids) throws Exception {
		List<XmlParser> responses = new ArrayList<>();
		for (String id : ids) {
			responses.add(execute(id));
		}
		return XmlParser.combineDocuments(responses).toString();
	}

	private XmlParser execute(String id) throws Exception {
		WebResource request = this.webResource.path(id + this.resource);
		ClientResponse response = request.accept("application/xml").get(ClientResponse.class);
		System.out.println("Query.execute.request: " + request);
		if (response.getStatus() != 200) {
			System.out.println("Query.execute.response: " + response.getEntity(String.class));
			System.out.println();
			throw new Exception("HTTP request failed: " + response.getStatus() + " - " + response.getStatusInfo() + " - "
					+ response.getEntity(String.class) + ", URL:" + this.webResource.getURI());
		}
		return new XmlParser(response.getEntity(String.class));
	}

	@Override
	public Set<String> projectReference(String queryResponse) throws XPathExpressionException {
		XmlParser responseDoc = new XmlParser(queryResponse);
		String query;
		if (this.projection.isEmpty()) {
			query = "//entry/link/@href";
		} else {
			String idAttr = this.projection.iterator().next();
			query = "//" + idAttr + "/@href";
		}
		Set<String> results = new HashSet<>(responseDoc.query(query));
		return results;
	}

	@Override
	public Set<String> projectContent(String queryResponse) throws XPathExpressionException {
		XmlParser responseDoc = new XmlParser(queryResponse);
		String query;
		if (this.projection.isEmpty()) {
			query = "//entry/title/text()";
		} else {
			String idAttr = this.projection.iterator().next();
			query = "//" + idAttr + "/text()";
		}
		Set<String> results = new HashSet<>(responseDoc.query(query));
		return results;
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
