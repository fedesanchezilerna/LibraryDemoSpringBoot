package com.ilerna.xmlprocessor;

public class XmlProcessorConfig {

    private boolean indentOutput = true;
    private boolean omitXmlDeclaration = false;
    private String encoding = "UTF-8";

    public boolean isIndentOutput() {
        return indentOutput;
    }

    public void setIndentOutput(boolean indentOutput) {
        this.indentOutput = indentOutput;
    }

    public boolean isOmitXmlDeclaration() {
        return omitXmlDeclaration;
    }

    public void setOmitXmlDeclaration(boolean omitXmlDeclaration) {
        this.omitXmlDeclaration = omitXmlDeclaration;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
