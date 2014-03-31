package cz.cvut.fel.nalida;

import java.util.List;
import java.util.Properties;

import cz.cvut.fel.nalida.db.Query;
import cz.cvut.fel.nalida.db.QueryBuilder;
import cz.cvut.fel.nalida.db.QueryPlan;
import cz.cvut.fel.nalida.db.XmlParser;

public class Test {
	public static void main(String[] args) throws Exception {

		//		manually();
		withPlan();
	}

	private static void withPlan() throws Exception {

		Properties props = new Properties();
		props.load(Main.class.getClassLoader().getResourceAsStream("db.properties"));

		Query coursesQuery = new QueryBuilder(props).projection("content/instance/instructors/teacher").resource("courses")
				.constraint("code", "==", "BDT").build();
		Query teachersQuery = new QueryBuilder(props).collection(false).projection("content/lastName", "content/phone").build();

		QueryPlan queryPlan = new QueryPlan();
		queryPlan.addQuery(coursesQuery);
		queryPlan.addQuery(teachersQuery);
		List<String> result = queryPlan.execute();

		for (String response : result) {
			System.out.println(new XmlParser(response).toString());
		}
	}

	@SuppressWarnings("unused")
	private static void manually() throws Exception {
		Properties props = new Properties();
		props.load(Main.class.getClassLoader().getResourceAsStream("db.properties"));

		Query coursesQuery = new QueryBuilder(props).projection("content/instance/instructors/teacher").resource("courses")
				.constraint("code", "==", "BDT").build();
		System.out.println(coursesQuery.toString());
		String coursesResponse = coursesQuery.execute();

		XmlParser coursesDoc = new XmlParser(coursesResponse);
		System.out.println(coursesDoc);

		String idAttr = "instance/instructors/teacher";
		List<String> results = coursesDoc.query("//content/" + idAttr + "/@href");
		System.out.println("Intermediate result: '" + results + "'");

		Query teachersQuery = new QueryBuilder(props).collection(false).projection("title", "link", "content/lastName", "content/phone")
				.build();
		System.out.println(teachersQuery);
		List<String> teachersResponse = teachersQuery.execute(results);

		for (String response : teachersResponse) {
			System.out.println(new XmlParser(response).toString());
		}
	}

}
