package cz.cvut.fel.nalida.db;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Pretty-prints xml, supplied as a string.
 * <p/>
 * eg.
 * <code>
 * String formattedXml = new XmlFormatter().format("<tag><nested>hello</nested></tag>");
 * </code>
 */
public class XmlParser {
	final Document document;

	public XmlParser(String document) {
		this.document = parseXmlFile(document);
	}

	public XmlParser(Document document) {
		this.document = document;
	}

	private Document parseXmlFile(String document) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(document));
			return db.parse(is);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static XmlParser combineDocuments(List<XmlParser> documents) {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootSetElement = doc.createElement("results");
			Node rootSetNode = doc.appendChild(rootSetElement);
			for (XmlParser d : documents) {
				rootSetNode.appendChild(doc.importNode(d.document.getFirstChild(), true));
			}
			return new XmlParser(doc);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String toString() {
		try {
			OutputFormat format = new OutputFormat(this.document);
			format.setLineWidth(65);
			format.setIndenting(true);
			format.setIndent(2);
			Writer out = new StringWriter();
			XMLSerializer serializer = new XMLSerializer(out, format);
			serializer.serialize(this.document);
			return out.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public List<String> query(String xpathQuery) throws XPathExpressionException {
		XPathFactory xPathfactory = XPathFactory.newInstance();
		XPath xpath = xPathfactory.newXPath();
		XPathExpression expr = xpath.compile(xpathQuery);
		NodeList nlist = (NodeList) expr.evaluate(this.document, XPathConstants.NODESET);
		List<String> resultList = new ArrayList<String>();
		for (int i = 0; i < nlist.getLength(); i++) {
			resultList.add(nlist.item(i).getNodeValue());
		}
		return resultList;
	}

}