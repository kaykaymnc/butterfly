package com.paypal.butterfly.utilities.misc;

import com.paypal.butterfly.extensions.api.TUExecutionResult;
import com.paypal.butterfly.extensions.api.TransformationContext;
import com.paypal.butterfly.extensions.api.TransformationUtility;
import com.paypal.butterfly.extensions.api.exception.TransformationUtilityException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses a Java web deployment descriptor file (web.xml),
 * identifies all context parameters, and save them into a map, the key
 * being param-name and the value being param-value.
 * <br>
 * <strong>Note: this utility does not validate the file's schema and content,
 * other than what it takes to identify all context-param elements at the document
 * element, and also their respective param-name and param-value elements</strong>
 *
 * @author facarvalho
 */
public class WebXmlContextParams extends TransformationUtility<WebXmlContextParams> {

    private static final String DESCRIPTION = "Parses Java web deployment descriptor file (%s), identifies all context parameters, and save them into a map";

    // Even though it is redundant to have this default constructor here, since it is
    // the only one (the compiler would have added it implicitly), this is being explicitly
    // set here to emphasize that the public default constructor should always be
    // available by any transformation utility even when additional constructors are present.
    // The reason for that is the fact that one or more of its properties might be set
    // during transformation time, using the TransformationUtility set method
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public WebXmlContextParams() {
    }

    @Override
    public String getDescription() {
        return String.format(DESCRIPTION, getRelativePath());
    }

    @Override
    protected TUExecutionResult execution(File transformedAppFolder, TransformationContext transformationContext) {
        File xmlFile = getAbsoluteFile(transformedAppFolder, transformationContext);
        Map<String, String> map = new HashMap<>();
        boolean warn = false;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            Element webAppNode = doc.getDocumentElement();
            NodeList contextParams = webAppNode.getElementsByTagName("context-param");

            Node contextParam;
            String element1Tag;
            String element1Content;
            String element2Tag;
            String element2Content;
            String paramName;
            String paramValue;

            for (int i = 0; i < contextParams.getLength(); i++) {
                contextParam = contextParams.item(i);

                element1Tag = contextParam.getFirstChild().getNextSibling().getNodeName();
                element1Content = contextParam.getFirstChild().getNextSibling().getTextContent();
                element2Tag = contextParam.getFirstChild().getNextSibling().getNextSibling().getNextSibling().getNodeName();
                element2Content = contextParam.getFirstChild().getNextSibling().getNextSibling().getNextSibling().getTextContent();

                // Setting param-name
                if ("param-name".equals(element1Tag)) {
                    paramName = element1Content;
                } else if ("param-name".equals(element2Tag)) {
                    paramName = element2Content;
                } else {
                    warn = true;
                    continue;
                }

                // Setting param-value
                if ("param-value".equals(element1Tag)) {
                    paramValue = element1Content;
                } else if ("param-value".equals(element2Tag)) {
                    paramValue = element2Content;
                } else {
                    warn = true;
                    continue;
                }

                map.put(paramName, paramValue);
            }
        } catch (SAXException |IOException |ParserConfigurationException e) {
            TransformationUtilityException tuex = new TransformationUtilityException("Exception happened when searching context parameters in web.xml file", e);
            return TUExecutionResult.error(this, tuex);
        }

        TUExecutionResult result = TUExecutionResult.value(this, map);
        if (warn) {
            result.addWarning(new TransformationUtilityException("This web.xml file has one or more not well formed context-param elements"));
        }

        return result;
    }

}
