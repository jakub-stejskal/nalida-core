package cz.cvut.fel.nalida.syntax;

import java.util.List;

public interface Lemmatizer {

	List<String> getLemmas(String text);

}
