package com.wgu.app;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpression;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

/**
 * User authentication service using XML-based user storage.
 */
public class UserService {
    
    private static final String XML_DATA = 
        "<?xml version=\"1.0\"?>" +
        "<users>" +
        "  <user>" +
        "    <username>admin</username>" +
        "    <password>secret123</password>" +
        "    <role>administrator</role>" +
        "  </user>" +
        "  <user>" +
        "    <username>john</username>" +
        "    <password>password</password>" +
        "    <role>user</role>" +
        "  </user>" +
        "</users>";
    
    /**
     * Authenticates a user by checking username and password.
     */
    public boolean authenticate(String username, String password) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(XML_DATA)));
            
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();
            
            String expression = "//user[username='" + username + 
                              "' and password='" + password + "']";
            
            XPathExpression expr = xpath.compile(expression);
            Object result = expr.evaluate(doc);
            
            return result != null && result.toString().length() > 0;
        } catch (Exception e) {
            System.err.println("Authentication error: " + e.getMessage());
            return false;
        }
    }
}

