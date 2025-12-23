package com.wgu.app;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathVariableResolver;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Secure user service that avoids XPath injection by using parameterized XPath with variables
 */
public class UserService {

    private final Document usersDoc;

    public UserService(InputStream xmlStream) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // prevent XXE
            dbf.setExpandEntityReferences(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            this.usersDoc = db.parse(xmlStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load users XML", e);
        }
    }

    /**
     * Authenticate a user using a parameterized XPath expression (no string concatenation).
     */
    public boolean authenticate(String username, String password) {
        try {
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xpath = xpf.newXPath();

            // Bind variables safely instead of concatenating user input
            SimpleVariableResolver resolver = new SimpleVariableResolver();
            resolver.addVariable(new QName("u"), username);
            resolver.addVariable(new QName("p"), password);
            xpath.setXPathVariableResolver(resolver);

            XPathExpression expr = xpath.compile("//user[username/text()=$u and password/text()=$p]");
            Node userNode = (Node) expr.evaluate(usersDoc, XPathConstants.NODE);
            return userNode != null;
        } catch (Exception e) {
            throw new RuntimeException("Authentication failed", e);
        }
    }

    /**
     * Simple variable resolver for XPath variables.
     */
    private static class SimpleVariableResolver implements XPathVariableResolver {
        private final Map<QName, Object> vars = new HashMap<>();

        void addVariable(QName name, Object value) {
            vars.put(name, value);
        }

        @Override
        public Object resolveVariable(QName variableName) {
            return vars.get(variableName);
        }
    }
}

