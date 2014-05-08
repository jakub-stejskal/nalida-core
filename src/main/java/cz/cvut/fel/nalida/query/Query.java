package cz.cvut.fel.nalida.query;

import java.util.Set;

import javax.xml.xpath.XPathExpressionException;

public interface Query {

	String execute() throws Exception;

	String execute(Set<String> queryParams) throws Exception;

	Set<String> projectReference(String queryResponse) throws XPathExpressionException;

	Set<String> projectContent(String queryResponse) throws XPathExpressionException;

}
