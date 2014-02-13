package cz.cvut.fel.nlidb4kos;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.DocDateAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.time.TimeAnnotator;

public class SyntacticAnalysis {

	private final StanfordCoreNLP pipeline;

	public SyntacticAnalysis(Properties properties) {
		pipeline = new StanfordCoreNLP(properties);
		pipeline.addAnnotator(new TimeAnnotator("sutime", properties));
	}

	/**	
	 * @param args
	 */
	public static void main(String[] args) {
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution 
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");

		SyntacticAnalysis sa = new SyntacticAnalysis(props);

		String text = "Which students attend machine learning course on tuesdays.";
		Annotation annotation = sa.process(text);

		Main.printSyntacticInfo(annotation);
	}

	public Annotation process(String text) {

		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);

		DateFormat df = new SimpleDateFormat("YYYY-MM-dd");
		String date = df.format(Calendar.getInstance().getTime());
		document.set(DocDateAnnotation.class, date);

		// run all Annotators on this text
		pipeline.annotate(document);

		return document;
	}
}
