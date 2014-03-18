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
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;

import cz.cvut.fel.nalida.Lemmatizer;
import cz.cvut.fel.nalida.Token;

public class Lexicon {

	Schema schema;
	Map<String, SemSet> lexicon;
	Set<Element> elements;
	Lemmatizer lemmatizer;

	public class SemSet extends HashSet<Token> {
		private static final long serialVersionUID = 1L;
	}

	public enum ElementType {
		ATTRIBUTE, ENTITY, VALUE, WH_WORD;
	}

	private static final String SCHEMA_FILENAME = "schema.desc";

	public Lexicon(String schemaPath) throws IOException {
		this.schema = loadSchema(schemaPath);
		this.lexicon = new HashMap<>();
		this.elements = new HashSet<>();
		this.lemmatizer = new Lemmatizer();
		loadLexicon(schemaPath);
	}

	private Schema loadSchema(String schemaPath) throws FileNotFoundException {
		InputStream input = new FileInputStream(new File(schemaPath + SCHEMA_FILENAME));
		return new Yaml().loadAs(input, Schema.class);
	}

	private void loadLexicon(String schemaPath) throws IOException {
		for (Entity entity : this.schema.getSchema()) {
			loadEntity(entity);
			for (Attribute attribute : entity.getAttributes()) {
				loadAttribute(entity, attribute);
				loadValues(schemaPath, entity, attribute);
			}
		}
		loadCommons(schemaPath);
	}

	private void loadEntity(Entity entity) {
		String token = entity.getName().toLowerCase();
		putToSemSet(token, entity.getName(), ElementType.ENTITY);
	}

	private void loadAttribute(Entity entity, Attribute attribute) {
		String name = attribute.getName();
		for (String token : attribute.getTokens()) {
			putToSemSet(token, entity.getName() + "." + name, ElementType.ATTRIBUTE);
		}
	}

	private void loadValues(String schemaPath, Entity entity, Attribute attribute) throws IOException {
		if (attribute.isPrimitiveType()) {
			String valueFilename = format("%s%s.%s.values", schemaPath, entity.getName(), attribute.getName());

			BufferedReader br = new BufferedReader(new FileReader(valueFilename));
			String line;
			while ((line = br.readLine()) != null) {
				for (String token : this.lemmatizer.getLemmas(line)) {
					putToSemSet(token, entity.getName() + "." + attribute.getName(), ElementType.VALUE);
				}
			}
			br.close();
		}
	}

	private void loadCommons(String schemaPath) throws IOException {
		String valueFilename = format("%s%s.%s.values", schemaPath, "common", "wh");

		BufferedReader br = new BufferedReader(new FileReader(valueFilename));
		String token;
		while ((token = br.readLine()) != null) {
			putToSemSet(token, token, ElementType.WH_WORD);
		}
		br.close();
	}

	private void putToSemSet(String token, String name, ElementType type) {
		SemSet semSet = this.lexicon.get(token);
		if (semSet == null) {
			semSet = new SemSet();
			this.lexicon.put(token, semSet);
		}
		semSet.add(new Token(type, name));
	}

	public SemSet getSemSet(String lemma) {
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
