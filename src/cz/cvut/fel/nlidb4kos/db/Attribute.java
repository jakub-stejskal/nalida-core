package cz.cvut.fel.nlidb4kos.db;

import java.util.List;

public class Attribute {
	private String name;
	private String type;
	private List<String> tokens;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setTokens(List<String> tokens) {
		this.tokens = tokens;
	}

	public List<String> getTokens() {
		return tokens;
	}

	@Override
	public String toString() {
		return "myNameTokens:" + name + "," + tokens;
	}
}
