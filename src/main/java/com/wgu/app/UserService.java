package com.wgu.app;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathVariableResolver;
import org.w3c.dom.Document;
import javax.xml.namespace.QName;
import java.io.InputStream;

/**
 * UserService with safe XPath usage to prevent XPath injection.
 */
public class UserService {

    /**
     * Authenticates a user by safely evaluating an XPath expression with variables.
     * This avoids concatenating untrusted input into the XPath string.
     */
    public boolean authenticate(String username, String password) {
        try {
            // Load users XML from classpath (adjust if different source is used)
            InputStream xmlStream = getClass().getResourceAsStream("/users.xml");
            if (xmlStream == null) {
                return false; // or throw
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // prevent XXE
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            dbf.setNamespaceAware(true);

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(xmlStream);

            XPathFactory xpf = XPathFactory.newInstance();
            XPath xpath = xpf.newXPath();

            // Bind variables instead of concatenation
            xpath.setXPathVariableResolver(new SafeVariableResolver()
                    .bind("username", username)
                    .bind("password", password));

            String expr = "/users/user[username = $username and password = $password]";
            XPathExpression compiled = xpath.compile(expr);
            Object result = compiled.evaluate(doc, XPathConstants.NODE);
            return result != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Simple variable resolver with immutable bindings.
     */
    static class SafeVariableResolver implements XPathVariableResolver {
        private java.util.Map<QName, Object> vars = new java.util.HashMap<>();

        public SafeVariableResolver bind(String name, Object value) {
            vars.put(new QName(name), value == null ? "" : value.toString());
            return this;
        }

        @Override
        public Object resolveVariable(QName variableName) {
            return vars.getOrDefault(variableName, "");
        }
    }
}

