package cz.cvut.fel.nalida.tokenization;

import java.util.Set;

public interface Tokenizer<T> {

	Set<Tokenization> getTokenizations(T annotatedLine);

}
