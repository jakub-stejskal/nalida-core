package cz.cvut.fel.nalida.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Joiner;

public class QueryPlan {
	List<Query> queries = new ArrayList<>();

	public void addQuery(Query query) {
		this.queries.add(query);
	}

	public String execute() throws Exception {
		List<String> queryParams = Collections.emptyList();
		String queryResponse = null;
		for (Query query : this.queries) {
			queryResponse = query.execute(queryParams);
			queryParams = query.projectReference(queryResponse);
		}
		return queryResponse;
	}

	@Override
	public String toString() {
		return Joiner.on("\n").join(this.queries);
	}
}
