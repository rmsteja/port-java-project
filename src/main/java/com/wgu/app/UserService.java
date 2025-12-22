package com.wgu.app;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathVariableResolver;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import java.io.InputStream;

/**
 * Secure UserService that avoids XPath injection by using variable binding
 * instead of concatenating untrusted input into XPath expressions.
 */
public class UserService {
    private final Document usersDoc;

    public UserService(InputStream usersXml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // Harden XML parser against XXE
            dbf.setExpandEntityReferences(false);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            this.usersDoc = dbf.newDocumentBuilder().parse(usersXml);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load users XML", e);
        }
    }

    /**
     * Authenticate safely using an XPathExpression with variables.
     * This avoids injection by not concatenating user input into the query.
     */
    public boolean authenticate(String username, String password) {
        try {
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xpath = xpf.newXPath();

            // Bind variables used in the XPath expression
            xpath.setXPathVariableResolver(new XPathVariableResolver() {
                @Override
                public Object resolveVariable(QName qname) {
                    String name = qname.getLocalPart();
                    if ("username".equals(name)) return username;
                    if ("password".equals(name)) return password;
                    return null;
                }
            });

            // Compile a parameterized XPath expression
            XPathExpression expr = xpath.compile(
                "//user[normalize-space(username/text())=$username and normalize-space(password/text())=$password]"
            );

            NodeList nodes = (NodeList) expr.evaluate(usersDoc, XPathConstants.NODESET);
            return nodes != null && nodes.getLength() > 0;
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Authentication failed", e);
        }
    }
}

