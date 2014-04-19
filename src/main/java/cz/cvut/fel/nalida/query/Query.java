package cz.cvut.fel.nalida.query;

import java.util.List;

import javax.xml.xpath.XPathExpressionException;

public interface Query {

	String execute() throws Exception;

	String execute(List<String> queryParams) throws Exception;

	List<String> projectReference(String queryResponse) throws XPathExpressionException;

	List<String> projectContent(String queryResponse) throws XPathExpressionException;

}
