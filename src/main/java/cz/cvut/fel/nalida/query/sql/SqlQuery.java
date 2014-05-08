package cz.cvut.fel.nalida.query.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;

import cz.cvut.fel.nalida.query.Query;

public class SqlQuery implements Query {

	private final Set<String> entities;
	private final Set<String> projection;
	private final Set<String> constraints;
	private String query;

	public SqlQuery(Set<String> entities, Set<String> projection, Set<String> constraints) {
		this.entities = entities;
		this.projection = projection;
		this.constraints = constraints;

		build();
	}

	private void build() {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ");
		if (this.projection.isEmpty()) {
			sb.append("*");
		} else {
			sb.append(Joiner.on(", ").join(this.projection));
		}
		sb.append(" FROM ");
		sb.append(Joiner.on(", ").join(this.entities));
		sb.append(" WHERE ");
		sb.append(Joiner.on(" AND ").join(this.constraints));
		sb.append(";");

		this.query = sb.toString();
	}

	@Override
	public String execute() throws Exception {
		return execute("");
	}

	@Override
	public String execute(Set<String> ids) throws Exception {
		List<String> responses = new ArrayList<>();
		if (ids.isEmpty()) {
			responses.add(execute(""));
		} else {
			for (String id : ids) {
				responses.add(execute(id));
			}
		}
		return responses.toString();
	}

	private String execute(String id) throws Exception {
		return toString();
	}

	@Override
	public Set<String> projectReference(String queryResponse) {
		return Collections.emptySet();
	}

	@Override
	public Set<String> projectContent(String queryResponse) {
		return Collections.emptySet();
	}

	@Override
	public String toString() {
		return this.query;
	}
}
