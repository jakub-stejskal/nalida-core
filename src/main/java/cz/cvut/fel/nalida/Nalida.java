package cz.cvut.fel.nalida;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import cz.cvut.fel.nalida.interpretation.Interpretation;
import cz.cvut.fel.nalida.interpretation.Interpreter;
import cz.cvut.fel.nalida.query.QueryPlan;
import cz.cvut.fel.nalida.query.rest.RestQueryGenerator;
import cz.cvut.fel.nalida.query.sql.SqlQueryGenerator;
import cz.cvut.fel.nalida.schema.Schema;
import cz.cvut.fel.nalida.syntax.stanford.SyntacticAnalysis;
import cz.cvut.fel.nalida.tokenization.stanford.StanfordInterpreter;
import edu.stanford.nlp.pipeline.Annotation;

public class Nalida {

	private static final String SCHEMA_PATH = "data/schema2/";
	private final Schema schema;
	private final SyntacticAnalysis syntacticAnalysis;
	private final Interpreter<Annotation> tokenizer;
	private final RestQueryGenerator restQueryGenerator;
	private final SqlQueryGenerator sqlQueryGenerator;

	public Nalida() throws IOException {
		this(loadPropsFromFile("db.properties"), SCHEMA_PATH + "schema.desc", SCHEMA_PATH);
	}

	public Nalida(Properties dbProps, String schemaPath, String valuesPath) throws IOException {

		InputStream input = new FileInputStream(new File(schemaPath));
		this.schema = Schema.load(input);
		Lexicon lexicon = new Lexicon(this.schema, valuesPath);

		this.syntacticAnalysis = new SyntacticAnalysis(lexicon);
		this.tokenizer = new StanfordInterpreter(lexicon);

		this.restQueryGenerator = new RestQueryGenerator(this.schema, dbProps);
		this.sqlQueryGenerator = new SqlQueryGenerator(this.schema, dbProps);
	}

	private static Properties loadPropsFromFile(String filePath) throws IOException {
		Properties props = new Properties();
		props.load(Nalida.class.getClassLoader().getResourceAsStream(filePath));
		return props;
	}

	public Set<Interpretation> getInterpretations(String query) {
		Annotation annotatedQuery = this.syntacticAnalysis.process(query);
		Set<Interpretation> interpretations = this.tokenizer.interpret(annotatedQuery);
		return interpretations;
	}

	public QueryPlan getRestQuery(Interpretation interpretation) {
		return this.restQueryGenerator.generateQuery(interpretation);
	}

	public QueryPlan getSqlQuery(Interpretation interpretation) {
		return this.sqlQueryGenerator.generateQuery(interpretation);
	}

	public Schema getSchema() {
		return this.schema;
	}
}
