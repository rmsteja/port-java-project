package com.wgu.app;

import javax.xml.xpath.*;
import javax.xml.namespace.QName;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import java.util.HashMap;
import java.util.Map;

/**
 * Secure UserService with parameterized XPath to prevent XPath injection.
 */
public class UserService {
    private final XPathFactory xpathFactory = XPathFactory.newInstance();

    /**
     * Authenticates a user against the provided users XML Document.
     * This method avoids XPath injection by binding variables rather than concatenating user input.
     */
    public boolean authenticate(Document usersDoc, String username, String password) throws XPathExpressionException {
        if (usersDoc == null) return false;
        if (username == null || password == null) return false;

        // Prepare XPath with variable resolver
        XPath xpath = xpathFactory.newXPath();
        Map<QName, Object> vars = new HashMap<>();
        vars.put(new QName("username"), username);
        vars.put(new QName("password"), password);
        xpath.setXPathVariableResolver(vars::get);

        // Use normalized text and variables to avoid injection and whitespace issues
        XPathExpression expr = xpath.compile("//user[normalize-space(username/text())=$username and normalize-space(password/text())=$password]");
        NodeList nodes = (NodeList) expr.evaluate(usersDoc, XPathConstants.NODESET);
        return nodes != null && nodes.getLength() > 0;
    }
}

