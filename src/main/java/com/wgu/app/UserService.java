package com.wgu.app;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import java.io.InputStream;

/**
 * Secure UserService implementation to prevent XPath Injection.
 * This version ensures user input is safely embedded into XPath using a proper
 * literal builder and hardened XML parser configuration.
 */
public class UserService {
    private final Document userDoc;

    /**
     * Construct with a users XML InputStream.
     * The XML parser is configured to prevent XXE and external entities.
     */
    public UserService(InputStream usersXml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // Harden parser against XXE
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setExpandEntityReferences(false);
            dbf.setXIncludeAware(false);
            dbf.setNamespaceAware(true);

            DocumentBuilder db = dbf.newDocumentBuilder();
            this.userDoc = db.parse(usersXml);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load users XML", e);
        }
    }

    /**
     * Authenticate by matching username and password nodes in the XML.
     * User inputs are converted to safe XPath string literals to avoid injection.
     */
    public boolean authenticate(String username, String password) {
        try {
            String safeUser = toXPathLiteral(username);
            String safePass = toXPathLiteral(password);

            XPath xpath = XPathFactory.newInstance().newXPath();
            String expr = "//users/user[normalize-space(username/text())=" + safeUser
                        + " and normalize-space(password/text())=" + safePass + "]";
            XPathExpression compiled = xpath.compile(expr);
            NodeList nodes = (NodeList) compiled.evaluate(userDoc, XPathConstants.NODESET);
            return nodes != null && nodes.getLength() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Build a safe XPath string literal from arbitrary Java string.
     * - If it contains only single quotes, wrap in double quotes.
     * - If it contains only double quotes, wrap in single quotes.
     * - If it contains both, use concat with piecewise quoting to avoid breaking the XPath.
     */
    static String toXPathLiteral(String s) {
        if (s == null) {
            return "''"; // empty string literal
        }
        // Trim to prevent hidden bypass via leading/trailing spaces
        s = s.trim();
        if (s.indexOf('\'') == -1) {
            // no single quotes => safe to wrap in single quotes
            return "'" + s + "'";
        }
        if (s.indexOf('"') == -1) {
            // no double quotes => safe to wrap in double quotes
            return '"' + s + '"';
        }
        // Contains both quote types => build concat("part", "'", "part", ...)
        StringBuilder out = new StringBuilder("concat(");
        boolean first = true;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                if (start < i) {
                    if (!first) out.append(", ");
                    out.append('"').append(s.substring(start, i).replace("\"", "\\\"")).append('"');
                    first = false;
                }
                if (!first) out.append(", ");
                out.append('"').append("'"
                ).append('"');
                first = false;
                start = i + 1;
            }
        }
        if (start <= s.length()) {
            String tail = s.substring(start);
            if (!tail.isEmpty()) {
                if (!first) out.append(", ");
                out.append('"').append(tail.replace("\"", "\\\"")).append('"');
                first = false;
            }
        }
        out.append(')');
        return out.toString();
    }
}

