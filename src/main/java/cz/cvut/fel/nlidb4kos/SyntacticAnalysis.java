package cz.cvut.fel.nlidb4kos;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

import cz.cvut.fel.nlidb4kos.db.Lexicon;
import cz.cvut.fel.nlidb4kos.stanford.SemanticAnnotator;
import edu.stanford.nlp.ling.CoreAnnotations.DocDateAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.time.TimeAnnotator;

public class SyntacticAnalysis {

	private final StanfordCoreNLP pipeline;

	public SyntacticAnalysis(Properties properties, Lexicon lexicon) {
		this.pipeline = new StanfordCoreNLP(properties);
		this.pipeline.addAnnotator(new TimeAnnotator("sutime", properties));
		this.pipeline.addAnnotator(new SemanticAnnotator("semantic", properties, lexicon));
	}

	public Annotation process(String text) {

		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);

		DateFormat df = new SimpleDateFormat("YYYY-MM-dd");
		String date = df.format(Calendar.getInstance().getTime());
		document.set(DocDateAnnotation.class, date);

		// run all Annotators on this text
		this.pipeline.annotate(document);

		return document;
	}

	/**	
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution 
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");

		Lexicon lexicon = new Lexicon("data/schema/");
		SyntacticAnalysis sa = new SyntacticAnalysis(props, lexicon);

		String text = "Which students attend machine learning course on tuesdays.";
		Annotation annotation = sa.process(text);

		Main.printSyntacticInfo(annotation);
	}
}
