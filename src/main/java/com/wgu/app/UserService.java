package com.wgu.app;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathVariableResolver;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Secure UserService that prevents XPath injection by avoiding string concatenation
 * and using XPath variables with a custom resolver. Also hardens XML parsing
 * against XXE by disabling external entities.
 */
public class UserService {

    /**
     * Authenticates a user by safely querying the users XML using XPath variables.
     *
     * @param username the username provided by the user
     * @param password the password provided by the user
     * @return true if the user exists with the given credentials, false otherwise
     */
    public boolean authenticate(String username, String password) {
        // Basic null checks
        if (username == null || password == null) {
            return false;
        }

        try {
            // Securely configure the XML parser to mitigate XXE
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            try {
                dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            } catch (IllegalArgumentException ignored) {
                // Some parsers may not support these attributes; continue with defaults
            }
            dbf.setExpandEntityReferences(false);

            DocumentBuilder builder = dbf.newDocumentBuilder();

            // Load users.xml from classpath (adjust path if needed)
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("users.xml")) {
                if (is == null) {
                    return false; // users.xml not found
                }
                Document doc = builder.parse(is);

                XPathFactory xpf = XPathFactory.newInstance();
                XPath xpath = xpf.newXPath();

                // Provide variables via a resolver so user input is not concatenated
                Map<QName, Object> vars = new HashMap<>();
                vars.put(new QName("username"), username);
                vars.put(new QName("password"), password);
                xpath.setXPathVariableResolver(new MapVariableResolver(vars));

                // Compile an expression that uses variables instead of concatenation
                XPathExpression expr = xpath.compile("/users/user[username=$username and password=$password]");
                NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                return nodes != null && nodes.getLength() > 0;
            }
        } catch (Exception e) {
            // Log appropriately in real code; avoid leaking sensitive details
            return false;
        }
    }

    /**
     * Simple variable resolver backed by a map.
     */
    static class MapVariableResolver implements XPathVariableResolver {
        private final Map<QName, Object> variables;

        MapVariableResolver(Map<QName, Object> variables) {
            this.variables = variables;
        }

        @Override
        public Object resolveVariable(QName variableName) {
            return variables.get(variableName);
        }
    }
}

