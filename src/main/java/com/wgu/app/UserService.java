package com.wgu.app;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.XMLConstants;
import javax.xml.xpath.*;
import org.w3c.dom.Document;
import javax.xml.namespace.QName;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * UserService with secure XPath usage to prevent XPath injection.
 * - Avoids string concatenation of user-supplied input in XPath queries
 * - Uses XPath variables via XPathVariableResolver
 * - Hardens XML parsing against XXE
 */
public class UserService {

    private final Document usersDoc;

    public UserService() {
        this.usersDoc = loadUsersXmlSecurely();
    }

    private Document loadUsersXmlSecurely() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // Harden parser against XXE and similar attacks
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setExpandEntityReferences(false);
            dbf.setXIncludeAware(false);
            dbf.setNamespaceAware(true);

            DocumentBuilder db = dbf.newDocumentBuilder();
            try (InputStream is = getClass().getResourceAsStream("/users.xml")) {
                if (is == null) {
                    throw new IllegalStateException("/users.xml not found on classpath");
                }
                return db.parse(is);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load users XML securely", e);
        }
    }

    /**
     * Authenticates a user by username and password using a parameterized XPath expression.
     * This prevents XPath injection by not concatenating untrusted input into the query.
     */
    public boolean authenticate(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        try {
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xpath = xpf.newXPath();

            // Provide user inputs as variables rather than concatenating into the XPath string
            Map<String, Object> vars = new HashMap<>();
            vars.put("username", username);
            vars.put("password", password);
            xpath.setXPathVariableResolver(new MapVariableResolver(vars));

            // Compile an expression that references variables
            XPathExpression expr = xpath.compile("//user[username=$username and password=$password]");

            Object result = expr.evaluate(usersDoc, XPathConstants.NODE);
            return result != null;
        } catch (XPathExpressionException e) {
            return false;
        }
    }

    /** Simple variable resolver backed by a Map */
    private static class MapVariableResolver implements XPathVariableResolver {
        private final Map<QName, Object> qnameMap = new HashMap<>();

        MapVariableResolver(Map<String, Object> vars) {
            for (Map.Entry<String, Object> entry : vars.entrySet()) {
                qnameMap.put(new QName(entry.getKey()), entry.getValue());
            }
        }

        @Override
        public Object resolveVariable(QName variableName) {
            return qnameMap.get(variableName);
        }
    }
}

