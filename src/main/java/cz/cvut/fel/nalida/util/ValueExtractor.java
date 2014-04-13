package cz.cvut.fel.nalida.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;

import cz.cvut.fel.nalida.Main;
import cz.cvut.fel.nalida.db.Attribute;
import cz.cvut.fel.nalida.db.Entity;
import cz.cvut.fel.nalida.db.Query;
import cz.cvut.fel.nalida.db.QueryBuilder;
import cz.cvut.fel.nalida.db.Schema;
import cz.cvut.fel.nalida.db.XmlParser;

public class ValueExtractor {
	Schema schema;
	int limit = 100;
	private final Properties props;
	private final String outputDir;

	public ValueExtractor(Schema schema, String outputDir, Properties props) {
		this.schema = schema;
		this.props = props;
		this.outputDir = outputDir;
		new File(this.outputDir).mkdirs();
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public void extractAll() {
		for (Entity entity : this.schema.getSchema()) {
			extractEntity(entity);
		}
	}

	public void extractEntity(Entity entity) {
		int offset = 0;
		boolean hasNext = true;
		while (hasNext) {
			offset += this.limit;
			hasNext = performQuery(entity, offset);
		}
	}

	private boolean performQuery(Entity entity, int offset) {
		Query query = new QueryBuilder(this.props).resource(entity.getResource()).projection("content").offset(offset).limit(this.limit)
				.build();

		try {
			String result = query.execute();
			XmlParser doc = new XmlParser(result);

			for (Attribute attribute : entity.getAttributes()) {
				List<String> values = doc.query("//" + attribute.getName() + "/text()");
				saveValues(attribute, values);
			}

			return doc.query("//link[@rel='next']") != null;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	private void saveValues(Attribute attribute, List<String> values) {
		String fileName = this.outputDir + attribute.getLongName() + ".values";
		try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)))) {
			for (String value : values) {
				out.println(value);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Saved " + attribute.getLongName() + ": " + values);
	}

	public static void main(String[] args) throws Exception {

		InputStream input = new FileInputStream(new File("data/schema/schema.desc"));
		Schema s = Schema.load(input);
		Properties props = new Properties();
		props.load(Main.class.getClassLoader().getResourceAsStream("db.properties"));
		props.put("baseUrl", s.getBaseUri());
		ValueExtractor extractor = new ValueExtractor(s, "data/extracted/", props);
		extractor.extractAll();
	}
}
