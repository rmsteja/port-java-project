package com.wgu.app;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathVariableResolver;
import java.io.InputStream;
import java.util.Objects;

/**
 * Secure UserService that avoids XPath injection by using parameterized XPath variables
 * and hardened XML parsing (disables XXE). This replaces any unsafe string-concatenated
 * XPath query construction.
 */
public class UserService {

    private final Document usersDoc;

    public UserService() {
        this.usersDoc = loadUsersDocumentSecurely();
    }

    /**
     * Authenticates a user by safely querying the XML document using XPath variables
     * rather than concatenating user input into the XPath string.
     *
     * @param username provided username
     * @param password provided password
     * @return true if a matching user exists, false otherwise
     */
    public boolean authenticateUser(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        // Trim to a reasonable length to mitigate abuse; adjust as needed
        String u = safeTrim(username, 256);
        String p = safeTrim(password, 256);

        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();

            // Use variables to avoid injecting raw input into the XPath source
            xPath.setXPathVariableResolver(new SimpleVariableResolver()
                    .withVariable("username", u)
                    .withVariable("password", p));

            XPathExpression expr = xPath.compile("//user[username=$username and password=$password]");
            NodeList nodes = (NodeList) expr.evaluate(usersDoc, XPathConstants.NODESET);
            return nodes != null && nodes.getLength() > 0;
        } catch (Exception e) {
            // Log in real code; avoid leaking details
            return false;
        }
    }

    private static String safeTrim(String s, int maxLen) {
        String t = s.trim();
        if (t.length() > maxLen) {
            t = t.substring(0, maxLen);
        }
        return t;
    }

    private Document loadUsersDocumentSecurely() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // Harden XML parsing: prevent XXE and enable secure processing
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setExpandEntityReferences(false);
            dbf.setXIncludeAware(false);

            DocumentBuilder db = dbf.newDocumentBuilder();
            try (InputStream is = getUsersXmlStream()) {
                Document doc = db.parse(Objects.requireNonNull(is, "users.xml not found"));
                doc.getDocumentElement().normalize();
                return doc;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load users XML securely", e);
        }
    }

    /**
     * Replace with your actual XML source loading (classpath, file, etc.).
     */
    private InputStream getUsersXmlStream() {
        // Assumes users.xml on classpath under /data/users.xml; adjust as needed
        return Thread.currentThread().getContextClassLoader().getResourceAsStream("data/users.xml");
    }

    /**
     * Simple resolver for XPath variables.
     */
    private static class SimpleVariableResolver implements XPathVariableResolver {
        private String username;
        private String password;

        public SimpleVariableResolver withVariable(String name, String value) {
            if ("username".equals(name)) {
                this.username = value;
            } else if ("password".equals(name)) {
                this.password = value;
            }
            return this;
        }

        @Override
        public Object resolveVariable(QName variableName) {
            String local = variableName.getLocalPart();
            if ("username".equals(local)) {
                return username;
            }
            if ("password".equals(local)) {
                return password;
            }
            return null;
        }
    }
}

