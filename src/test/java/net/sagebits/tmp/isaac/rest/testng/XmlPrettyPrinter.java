package net.sagebits.tmp.isaac.rest.testng;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * 
 * {@link XmlPrettyPrinter}
 *
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 *
 */
public class XmlPrettyPrinter {
	private static final Logger LOG = LogManager.getLogger(XmlPrettyPrinter.class);
	
	private XmlPrettyPrinter() {}

	public static String toString(String xml, int indent) {
	    try {
	        // Turn xml string into a document
	        Document document = DocumentBuilderFactory.newInstance()
	                .newDocumentBuilder()
	                .parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));

	        // Remove whitespaces outside tags
	        document.normalize();
	        XPath xPath = XPathFactory.newInstance().newXPath();
	        NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']",
	                                                      document,
	                                                      XPathConstants.NODESET);

	        for (int i = 0; i < nodeList.getLength(); ++i) {
	            Node node = nodeList.item(i);
	            node.getParentNode().removeChild(node);
	        }

	        // Setup pretty print options
	        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        transformerFactory.setAttribute("indent-number", indent);
	        Transformer transformer = transformerFactory.newTransformer();
	        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

	        // Return pretty print xml string
	        StringWriter stringWriter = new StringWriter();
	        transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
	        return stringWriter.toString();
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	}
	public static String toString(String xmlString) {
		try {
			return toString(xmlString, 4);
		} catch (Exception e) {
			// Try without DOM
		}
		try {
			/* Remove new lines */
			final String LINE_BREAK = "\n";
			xmlString = xmlString.replaceAll(LINE_BREAK, "");
			StringBuffer prettyPrintXml = new StringBuffer();
			/* Group the xml tags */
			Pattern pattern = Pattern.compile("(<[^/][^>]+>)?([^<]*)(</[^>]+>)?(<[^/][^>]+/>)?");
			Matcher matcher = pattern.matcher(xmlString);
			int tabCount = 0;
			while (matcher.find()) {
				String str1 = (null == matcher.group(1) || "null".equals(matcher.group())) ? "" : matcher.group(1);
				String str2 = (null == matcher.group(2) || "null".equals(matcher.group())) ? "" : matcher.group(2);
				String str3 = (null == matcher.group(3) || "null".equals(matcher.group())) ? "" : matcher.group(3);
				String str4 = (null == matcher.group(4) || "null".equals(matcher.group())) ? "" : matcher.group(4);

				if (matcher.group() != null && !matcher.group().trim().equals("")) {
					printTabs(tabCount, prettyPrintXml);
					if (!str1.equals("") && str3.equals("")) {
						++tabCount;
					}
					if (str1.equals("") && !str3.equals("")) {
						--tabCount;
						prettyPrintXml.deleteCharAt(prettyPrintXml.length() - 1);
					}

					prettyPrintXml.append(str1);
					prettyPrintXml.append(str2);
					prettyPrintXml.append(str3);
					if (!str4.equals("")) {
						prettyPrintXml.append(LINE_BREAK);
						printTabs(tabCount, prettyPrintXml);
						prettyPrintXml.append(str4);
					}
					prettyPrintXml.append(LINE_BREAK);
				}
			}
			return prettyPrintXml.toString();
		} catch (Throwable t) {
			LOG.warn("FAILED pretty-printing XML", t);
			return xmlString;
		}
	}

	private static void printTabs(int count, StringBuffer stringBuffer) {
		for (int i = 0; i < count; i++) {
			stringBuffer.append("\t");
		}
	}

	public static void main(String[] args) {
		String x = new String(
				"<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><soap:Fault><faultcode>soap:Client</faultcode><faultstring>INVALID_MESSAGE</faultstring><detail><ns3:XcbSoapFault xmlns=\"\" xmlns:ns3=\"http://www.someapp.eu/xcb/types/xcb/v1\"><CauseCode>20007</CauseCode><CauseText>INVALID_MESSAGE</CauseText><DebugInfo>Problems creating SAAJ object model</DebugInfo></ns3:XcbSoapFault></detail></soap:Fault></soap:Body></soap:Envelope>");
		System.out.println(toString(x));
	}
}