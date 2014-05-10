package cz.cvut.fel.nalida.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

public class QueryPlan {
	protected List<Query> queries = new ArrayList<>();

	public void addQuery(Query query) {
		this.queries.add(query);
	}

	public String execute() throws Exception {
		Set<String> queryParams = Sets.newHashSet("");
		String queryResponse = null;
		for (Query query : this.queries) {
			if (queryParams.isEmpty()) {
				return "Constraints of the query not satisfied: " + query;
			}
			queryResponse = query.execute(queryParams);
			queryParams = query.projectReference(queryResponse);
		}
		return queryResponse;
	}

	public int getLenght() {
		return this.queries.size();
	}

	@Override
	public String toString() {
		return Joiner.on("\n").join(this.queries);
	}
}
