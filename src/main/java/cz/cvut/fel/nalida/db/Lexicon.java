package cz.cvut.fel.nalida.db;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;

import cz.cvut.fel.nalida.Lemmatizer;
import cz.cvut.fel.nalida.Token;

public class Lexicon {

	Schema schema;
	Map<String, Set<Token>> lexicon;
	Map<String, Element> elements;
	Lemmatizer lemmatizer;

	private static final String SCHEMA_FILENAME = "schema.desc";

	public Lexicon(String schemaPath) throws IOException {
		this.schema = loadSchema(schemaPath);
		this.lexicon = new HashMap<>();
		this.elements = new HashMap<>();
		this.lemmatizer = new Lemmatizer();
		loadLexicon(schemaPath);
	}

	private Schema loadSchema(String schemaPath) throws FileNotFoundException {
		InputStream input = new FileInputStream(new File(schemaPath + SCHEMA_FILENAME));
		return Schema.load(input);
	}

	private void loadLexicon(String schemaPath) throws IOException {
		for (Entity entity : this.schema.getSchema()) {
			loadEntity(entity);
			for (Attribute attribute : entity.getAttributes()) {
				loadAttribute(entity, attribute);
				loadValues(schemaPath, entity, attribute);
			}
			for (Attribute attribute : entity.getSubresources()) {
				loadAttribute(entity, attribute);
			}
		}
		loadCommons(schemaPath);
	}

	private void loadEntity(Entity entity) {
		for (String token : entity.getTokens()) {
			putToSemSet(Lists.newArrayList(token), entity);
		}
	}

	private void loadAttribute(Entity entity, Attribute attribute) {
		for (String token : attribute.getTokens()) {
			putToSemSet(Lists.newArrayList(token), attribute);
		}
	}

	private void loadValues(String schemaPath, Entity entity, Attribute attribute) throws IOException {
		if (attribute.isPrimitiveType()) {
			String valueFilename = format("%s%s.%s.values", schemaPath, entity.getName(), attribute.getName());

			BufferedReader br = new BufferedReader(new FileReader(valueFilename));
			String line;
			while ((line = br.readLine()) != null) {
				putToSemSet(this.lemmatizer.getLemmas(line), attribute.getValueElement());

			}
			br.close();
		}
	}

	private void loadCommons(String schemaPath) throws IOException {
		String valueFilename = format("%s%s.%s.values", schemaPath, "common", "wh");

		BufferedReader br = new BufferedReader(new FileReader(valueFilename));
		String line;
		while ((line = br.readLine()) != null) {
			putToSemSet(this.lemmatizer.getLemmas(line), Element.WH_ELEMENT);
		}
		br.close();
	}

	private void putToSemSet(List<String> words, Element element) {
		for (String word : words) {
			Set<Token> tokenSet = this.lexicon.get(word);
			if (tokenSet == null) {
				tokenSet = new HashSet<>();
				this.lexicon.put(word, tokenSet);
			}
			tokenSet.add(new Token(words, element));
		}
	}

	public Set<Token> getSemSet(String lemma) {
		return this.lexicon.get(lemma.toLowerCase());
	}

	public Schema getSchema() {
		return this.schema;
	}

	@Override
	public String toString() {
		return "schema: \n" + this.schema + "\nlexicon: \n" + this.lexicon;
	}

	public static void main(String[] args) throws Exception {
		Lexicon l = new Lexicon("data/schema/");
		System.out.println(l);
	}

}
