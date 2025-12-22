package com.wgu.app;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;

/**
 * UserService with safe XPath usage to prevent XPath injection.
 * Avoids direct string concatenation of untrusted input into XPath queries
 * by converting input into safe XPath string literals and enabling secure XML processing.
 */
public class UserService {
    private final Document usersDoc;
    private final XPath xpath;

    public UserService() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // Harden XML parser
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            try {
                dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            } catch (IllegalArgumentException ignored) {
                // Attributes not supported by all JAXP implementations; safe to ignore
            }
            dbf.setExpandEntityReferences(false);
            dbf.setXIncludeAware(false);
            dbf.setNamespaceAware(false);

            DocumentBuilder db = dbf.newDocumentBuilder();
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("users.xml")) {
                if (is == null) {
                    throw new IllegalStateException("users.xml not found on classpath");
                }
                usersDoc = db.parse(is);
            }

            XPathFactory xpf = XPathFactory.newInstance();
            xpath = xpf.newXPath();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize UserService", e);
        }
    }

    /**
     * Authenticates a user against the users.xml data using a safe XPath query.
     *
     * @param username untrusted username input
     * @param password untrusted password input
     * @return true if a matching user node exists; false otherwise
     */
    public boolean authenticate(String username, String password) {
        try {
            String u = toXPathLiteral(username);
            String p = toXPathLiteral(password);
            // Build XPath using safe string literals instead of raw concatenation
            String expr = "//user[username/text()=" + u + " and password/text()=" + p + "]";
            XPathExpression xpe = xpath.compile(expr);
            Node node = (Node) xpe.evaluate(usersDoc, XPathConstants.NODE);
            return node != null;
        } catch (XPathExpressionException e) {
            // Any evaluation error results in authentication failure
            return false;
        }
    }

    /**
     * Converts an arbitrary Java string to a safe XPath string literal.
     * Handles values containing both single and double quotes by using concat().
     */
    static String toXPathLiteral(String s) {
        if (s == null) return "''"; // null becomes empty string literal
        if (!s.contains("'")) {
            return "'" + s + "'";
        }
        if (!s.contains("\"")) {
            return "\"" + s + "\"";
        }
        StringBuilder sb = new StringBuilder("concat(");
        boolean first = true;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' || c == '"') {
                String part = s.substring(start, i);
                if (!part.isEmpty()) {
                    if (!first) sb.append(", ");
                    sb.append('\'').append(part).append('\'');
                    first = false;
                }
                if (!first) sb.append(", ");
                if (c == '\'') {
                    sb.append("\"'\""); // double-quoted single quote
                } else {
                    sb.append("'\"'"); // single-quoted double quote
                }
                first = false;
                start = i + 1;
            }
        }
        String tail = s.substring(start);
        if (!tail.isEmpty()) {
            if (!first) sb.append(", ");
            sb.append('\'').append(tail).append('\'');
        }
        sb.append(')');
        return sb.toString();
    }
}

