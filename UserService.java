package com.port.security;

import org.w3c.dom.Document;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Node;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * UserService with mitigations against XPath Injection.
 *
 * Changes:
 * - Avoid direct string concatenation of untrusted input into XPath.
 * - Whitelist validation for username/password.
 * - Safe construction of XPath string literals via xpathLiteral().
 * - Secure XML parser configuration to prevent XXE.
 */
public class UserService {
    private static final Pattern SAFE_INPUT = Pattern.compile("^[A-Za-z0-9_@.-]{1,64}$");
    private final Document usersDoc;

    public UserService() {
        this.usersDoc = loadUsersDocumentSecurely("/users.xml");
    }

    // Authenticate user using safe XPath literal construction and input validation
    public boolean authenticateUser(String username, String password) {
        if (!isSafe(username) || !isSafe(password)) {
            return false; // reject potentially malicious input
        }
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            String expr = "//user[username=" + xpathLiteral(username) + " and password=" + xpathLiteral(password) + "]";
            XPathExpression compiled = xpath.compile(expr);
            Node node = (Node) compiled.evaluate(usersDoc, XPathConstants.NODE);
            return node != null;
        } catch (Exception e) {
            return false;
        }
    }

    // Build a safe XPath string literal, handling embedded quotes
    // See: https://www.w3.org/TR/xpath-10/#NT-Literal
    private static String xpathLiteral(String s) {
        if (s.indexOf('\'') == -1) {
            return "'" + s + "'";
        }
        if (s.indexOf('"') == -1) {
            return '"' + s + '"';
        }
        // If both quote types are present, use concat with pieces split on '
        StringBuilder sb = new StringBuilder();
        sb.append("concat(");
        boolean first = true;
        for (String part : s.split("'")) {
            if (!first) {
                sb.append(", '\"'\"', "); // append single quote character as '"'"'
            }
            sb.append("'" ).append(part).append("'");
            first = false;
        }
        sb.append(")");
        return sb.toString();
    }

    private static boolean isSafe(String s) {
        return s != null && SAFE_INPUT.matcher(s).matches();
    }

    // Securely load users.xml from classpath with XXE-safe parser settings
    private static Document loadUsersDocumentSecurely(String resourcePath) {
        try (InputStream in = UserService.class.getResourceAsStream(resourcePath)) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            dbf.setNamespaceAware(true);
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(in);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load users.xml securely", e);
        }
    }
}

