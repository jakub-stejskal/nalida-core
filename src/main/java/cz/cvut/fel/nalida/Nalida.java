package cz.cvut.fel.nalida;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import cz.cvut.fel.nalida.query.QueryPlan;
import cz.cvut.fel.nalida.query.rest.RestQueryGenerator;
import cz.cvut.fel.nalida.query.sql.SqlQueryGenerator;
import cz.cvut.fel.nalida.schema.Schema;
import cz.cvut.fel.nalida.syntax.stanford.SyntacticAnalysis;
import cz.cvut.fel.nalida.tokenization.Tokenization;
import cz.cvut.fel.nalida.tokenization.Tokenizer;
import cz.cvut.fel.nalida.tokenization.stanford.StanfordTokenizer;
import edu.stanford.nlp.pipeline.Annotation;

public class Nalida {

	private final Schema schema;
	private final SyntacticAnalysis syntacticAnalysis;
	private final Tokenizer<Annotation> tokenizer;
	private final RestQueryGenerator restQueryGenerator;
	private final SqlQueryGenerator sqlQueryGenerator;

	public Nalida() throws IOException {
		this(loadPropsFromFile("nlpcore.properties"), loadPropsFromFile("db.properties"), "data/schema/schema.desc", "data/schema/");
	}

	public Nalida(Properties nlpProps, Properties dbProps, String schemaPath, String valuesPath) throws IOException {

		InputStream input = new FileInputStream(new File(schemaPath));
		this.schema = Schema.load(input);
		Lexicon lexicon = new Lexicon(this.schema, valuesPath);

		this.syntacticAnalysis = new SyntacticAnalysis(nlpProps, lexicon);
		this.tokenizer = new StanfordTokenizer(lexicon);

		this.restQueryGenerator = new RestQueryGenerator(this.schema, dbProps);
		this.sqlQueryGenerator = new SqlQueryGenerator(this.schema, dbProps);
	}

	private static Properties loadPropsFromFile(String filePath) throws IOException {
		Properties props = new Properties();
		props.load(Nalida.class.getClassLoader().getResourceAsStream(filePath));
		return props;
	}

	public Set<Tokenization> getTokenizations(String query) {
		Annotation annotatedQuery = this.syntacticAnalysis.process(query);
		Set<Tokenization> tokenizations = this.tokenizer.getTokenizations(annotatedQuery);
		return tokenizations;
	}

	public QueryPlan getRestQuery(Tokenization tokenization) {
		return this.restQueryGenerator.generateQuery(tokenization);
	}

	public QueryPlan getSqlQuery(Tokenization tokenization) {
		return this.sqlQueryGenerator.generateQuery(tokenization);
	}

	public Schema getSchema() {
		return this.schema;
	}
}
