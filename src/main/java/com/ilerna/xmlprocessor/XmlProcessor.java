package com.ilerna.xmlprocessor;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

/**
 * Procesador XML con API para lectura, modificación y serialización de documentos XML.
 *
 * <p>Ejemplo de uso:</p>
 * <pre>
 * String result = new XmlProcessor()
 *     .read(xmlString)
 *     .update("/orders/order/status", "OK")
 *     .add("/orders/order", "note", "Procesado")
 *     .delete("/orders/order/oldField")
 *     .toString();
 * </pre>
 */
public class XmlProcessor {

    private Document document;
    private final XmlProcessorConfig config;

    public XmlProcessor() {
        this(new XmlProcessorConfig());
    }

    public XmlProcessor(XmlProcessorConfig config) {
        this.config = config;
    }

    // ── Lectura ──────────────────────────────────────────────

    public XmlProcessor read(String xml) {
        try {
            return read(new ByteArrayInputStream(xml.getBytes(config.getEncoding())));
        } catch (UnsupportedEncodingException e) {
            throw new XmlProcessorException("Encoding no soportado: " + config.getEncoding(), e);
        }
    }

    public XmlProcessor read(File file) {
        try {
            return read(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new XmlProcessorException("Fichero no encontrado: " + file.getPath(), e);
        }
    }

    public XmlProcessor read(InputStream inputStream) {
        try {
            DocumentBuilderFactory factory = createSecureDocumentBuilderFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();
            this.document = builder.parse(inputStream);
            this.document.getDocumentElement().normalize();
            return this;
        } catch (Exception e) {
            throw new XmlProcessorException("Error al parsear el XML", e);
        }
    }

    // ── Transformaciones ─────────────────────────────────────

    public XmlProcessor update(String xpath, String newValue) {
        ensureDocumentLoaded();
        NodeList nodes = evaluateXPath(xpath);
        if (nodes.getLength() == 0) {
            throw new XmlProcessorException("No se encontraron nodos para XPath: " + xpath);
        }
        for (int i = 0; i < nodes.getLength(); i++) {
            nodes.item(i).setTextContent(newValue);
        }
        return this;
    }

    public XmlProcessor delete(String xpath) {
        ensureDocumentLoaded();
        NodeList nodes = evaluateXPath(xpath);
        if (nodes.getLength() == 0) {
            throw new XmlProcessorException("No se encontraron nodos para XPath: " + xpath);
        }
        for (int i = nodes.getLength() - 1; i >= 0; i--) {
            Node node = nodes.item(i);
            node.getParentNode().removeChild(node);
        }
        return this;
    }

    public XmlProcessor add(String parentXpath, String elementName, String textContent) {
        ensureDocumentLoaded();
        NodeList parents = evaluateXPath(parentXpath);
        if (parents.getLength() == 0) {
            throw new XmlProcessorException("No se encontraron nodos padre para XPath: " + parentXpath);
        }
        for (int i = 0; i < parents.getLength(); i++) {
            Element newElement = document.createElement(elementName);
            if (textContent != null) {
                newElement.setTextContent(textContent);
            }
            parents.item(i).appendChild(newElement);
        }
        return this;
    }

    // ── Salida ───────────────────────────────────────────────

    public Document getDocument() {
        ensureDocumentLoaded();
        return document;
    }

    @Override
    public String toString() {
        ensureDocumentLoaded();
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, config.getEncoding());
            transformer.setOutputProperty(OutputKeys.INDENT, config.isIndentOutput() ? "yes" : "no");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
                    config.isOmitXmlDeclaration() ? "yes" : "no");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (TransformerException e) {
            throw new XmlProcessorException("Error al serializar el XML", e);
        }
    }

    // ── Métodos privados ─────────────────────────────────────

    private void ensureDocumentLoaded() {
        if (document == null) {
            throw new XmlProcessorException("No hay documento XML cargado. Llama a read() primero.");
        }
    }

    private NodeList evaluateXPath(String xpath) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            return (NodeList) xPath.evaluate(xpath, document, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new XmlProcessorException("Expresión XPath inválida: " + xpath, e);
        }
    }

    private DocumentBuilderFactory createSecureDocumentBuilderFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }
}
