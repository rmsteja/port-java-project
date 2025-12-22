package com.wgu.app;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathVariableResolver;
import javax.xml.namespace.QName;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class UserService {

    // Authenticate user using secure XPath with variable binding to prevent XPath injection
    public boolean authenticate(String username, String password) {
        try {
            // Build a secure XML parser to avoid XXE and other parser-level issues
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            DocumentBuilder db = dbf.newDocumentBuilder();

            // Load users XML from classpath (adjust if project stores it elsewhere)
            try (InputStream is = getClass().getResourceAsStream("/users.xml")) {
                if (is == null) {
                    return false; // users.xml not found
                }
                Document doc = db.parse(is);

                XPathFactory xpf = XPathFactory.newInstance();
                XPath xpath = xpf.newXPath();

                // Bind variables instead of concatenating user input into the XPath string
                Map<QName, Object> vars = new HashMap<>();
                vars.put(new QName("username"), username);
                vars.put(new QName("password"), password);

                XPathVariableResolver resolver = vars::get;
                xpath.setXPathVariableResolver(resolver);

                // Secure, parameterized XPath expression
                XPathExpression expr = xpath.compile("//user[username/text()=$username and password/text()=$password]");
                NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                return nodes != null && nodes.getLength() > 0;
            }
        } catch (Exception e) {
            // Log appropriately in real code; avoid leaking details
            return false;
        }
    }
}

