package cz.cvut.fel.nalida.db;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class Query {

	private final WebResource webResource;

	public Query(WebResource webResource) {
		this.webResource = webResource;
	}

	public String getResponse() throws Exception {
		ClientResponse response = this.webResource.accept("application/xml").get(ClientResponse.class);
		if (response.getStatus() != 200) {
			System.out.println(this.webResource);
			System.out.println(response.getEntity(String.class));
			System.out.println();
			throw new Exception("Failed : HTTP error code : " + response.getStatus() + " - " + response.getStatusInfo() + ", URL:" + this.webResource.getURI());
		}
		return response.getEntity(String.class);
	}

	@Override
	public String toString() {
		return this.webResource.toString();
	}
}
