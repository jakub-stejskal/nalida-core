package cz.cvut.fel.nalida.syntax.stanford;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

import cz.cvut.fel.nalida.Lexicon;
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
		Annotation document = new Annotation(text);
		DateFormat df = new SimpleDateFormat("YYYY-MM-dd");
		String date = df.format(Calendar.getInstance().getTime());
		document.set(DocDateAnnotation.class, date);

		this.pipeline.annotate(document);

		return document;
	}
}