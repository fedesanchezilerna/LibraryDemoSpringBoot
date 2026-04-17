package com.ilerna.xmlprocessor;

public class XmlProcessorException extends RuntimeException {

    public XmlProcessorException(String message) {
        super(message);
    }

    public XmlProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
}
