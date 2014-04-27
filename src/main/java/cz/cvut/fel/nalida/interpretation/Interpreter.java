package cz.cvut.fel.nalida.interpretation;

import java.util.Set;

public interface Interpreter<T> {

	Set<Interpretation> interpret(T annotatedLine);

}
