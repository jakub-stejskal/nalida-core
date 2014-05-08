package cz.cvut.fel.nalida.syntax.stanford;

import java.util.Properties;

import cz.cvut.fel.nalida.Lexicon;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;

public class SyntacticAnalysis {

	private final StanfordCoreNLP pipeline;

	public SyntacticAnalysis(Lexicon lexicon) {
		this(PropertiesUtils.fromString("annotators=tokenize, ssplit, pos, lemma, parse"), lexicon);
	}

	public SyntacticAnalysis(Properties properties, Lexicon lexicon) {
		this.pipeline = new StanfordCoreNLP(properties);
		this.pipeline.addAnnotator(new SemanticAnnotator("semantic", properties, lexicon));
	}

	public Annotation process(String text) {
		Annotation document = new Annotation(text);
		this.pipeline.annotate(document);
		return document;
	}
}
