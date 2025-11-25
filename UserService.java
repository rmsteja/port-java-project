package com.port.security;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * UserService with mitigations for XPath injection.
 * Avoids concatenating untrusted input directly into XPath by sanitizing inputs
 * with strict whitelisting and escaping, and by limiting the query to simple equality checks.
 */
public class UserService {

    // Allow only safe characters; adjust whitelist as needed for your usernames/passwords policy
    private static final Pattern SAFE_CHARS = Pattern.compile("^[A-Za-z0-9_@.-]+$");

    /**
     * Sanitize input used in XPath equality predicates by enforcing a strict whitelist
     * and escaping quotes to prevent breaking out of string literals.
     */
    private static String sanitizeForXPath(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        if (!SAFE_CHARS.matcher(trimmed).matches()) {
            // Reject or neutralize unsafe input; here we remove disallowed characters
            trimmed = trimmed.replaceAll("[^A-Za-z0-9_@.-]", "");
        }
        // Escape quotes to keep the XPath string literal intact
        return trimmed.replace("\"", "&quot;").replace("'", "&apos;");
    }

    /**
     * Authenticate a user against users.xml without XPath injection risk.
     * The XML is expected to have structure:
     * <users><user><username>...</username><password>...</password></user>...</users>
     */
    public boolean authenticate(String username, String password) {
        try {
            // Load XML securely
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // Prevent XXE
            dbf.setExpandEntityReferences(false);
            dbf.setNamespaceAware(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(Files.newInputStream(Paths.get("users.xml")));

            // Sanitize user-controlled values before using in XPath
            String userSafe = sanitizeForXPath(username);
            String passSafe = sanitizeForXPath(password);

            // Use a simple XPath equality check with sanitized literals
            XPath xp = XPathFactory.newInstance().newXPath();
            String expr = "//user[username/text()='" + userSafe + "' and password/text()='" + passSafe + "']";
            Node match = (Node) xp.evaluate(expr, doc, XPathConstants.NODE);
            return match != null;
        } catch (Exception e) {
            return false;
        }
    }
}

