package cz.cvut.fel.nlidb4kos;

import static org.junit.Assert.fail;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

public class SyntacticAnalysisTest {

	static SyntacticAnalysis sa;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");

		sa = new SyntacticAnalysis(props);
	}

	@Test
	public void testProcess() {
		fail("Not yet implemented");
	}

}
