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
import java.util.HashMap;
import java.util.Map;

/**
 * Secure UserService implementation that avoids XPath injection by using
 * variable binding instead of string concatenation.
 */
public class UserService {

    private final Document usersDoc;
    private final XPath xPath;

    public UserService(InputStream usersXmlStream) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // Harden XML parsing (avoid XXE/DTD)
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            dbf.setNamespaceAware(false);

            DocumentBuilder db = dbf.newDocumentBuilder();
            this.usersDoc = db.parse(usersXmlStream);

            XPathFactory xPathFactory = XPathFactory.newInstance();
            this.xPath = xPathFactory.newXPath();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize UserService", e);
        }
    }

    /**
     * Authenticates a user using variables in the XPath query to prevent injection.
     */
    public boolean authenticate(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        // Optional basic input constraints (defense-in-depth)
        if (!isReasonableIdentifier(username)) {
            return false;
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("u", username);
        vars.put("p", password);
        XPathExpression expr = compileWithVariables("count(/users/user[username=$u and password=$p])", vars);
        Double count = (Double) evaluate(expr, usersDoc, XPathConstants.NUMBER);
        return count != null && count.intValue() > 0;
    }

    /**
     * Fetches a user by id securely using variable binding.
     */
    public String getEmailByUserId(String userId) {
        if (userId == null || !isReasonableIdentifier(userId)) {
            return null;
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put("id", userId);
        XPathExpression expr = compileWithVariables("string(/users/user[@id=$id]/email)", vars);
        return (String) evaluate(expr, usersDoc, XPathConstants.STRING);
    }

    private XPathExpression compileWithVariables(String expression, Map<String, Object> variables) {
        try {
            xPath.setXPathVariableResolver(new MapVariableResolver(variables));
            return xPath.compile(expression);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compile XPath expression securely", e);
        }
    }

    private Object evaluate(XPathExpression expr, Object item, QName returnType) {
        try {
            return expr.evaluate(item, returnType);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to evaluate XPath expression", e);
        }
    }

    private boolean isReasonableIdentifier(String input) {
        // Basic allowlist: letters, digits, underscore, dot, dash; length 1..128
        return input.matches("[A-Za-z0-9_ .-]{1,128}");
    }

    /**
     * Simple variable resolver backed by a map.
     */
    private static class MapVariableResolver implements XPathVariableResolver {
        private final Map<QName, Object> vars = new HashMap<>();

        MapVariableResolver(Map<String, Object> source) {
            for (Map.Entry<String, Object> e : source.entrySet()) {
                vars.put(new QName(e.getKey()), e.getValue());
            }
        }

        @Override
        public Object resolveVariable(QName variableName) {
            return vars.get(variableName);
        }
    }
}

