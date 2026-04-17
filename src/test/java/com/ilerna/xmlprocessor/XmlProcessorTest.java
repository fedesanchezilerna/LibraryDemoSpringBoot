package com.ilerna.xmlprocessor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class XmlProcessorTest {

    private static final String SAMPLE_XML = """
            <orders>
                <order id="1">
                    <product>Laptop</product>
                    <quantity>2</quantity>
                    <status>PENDING</status>
                </order>
                <order id="2">
                    <product>Mouse</product>
                    <quantity>5</quantity>
                    <status>PENDING</status>
                </order>
            </orders>
            """;

    private XmlProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new XmlProcessor();
    }

    // ── Lectura ──────────────────────────────────────────────

    @Nested
    class ReadTests {

        @Test
        void readFromString() {
            XmlProcessor result = processor.read(SAMPLE_XML);
            assertNotNull(result);
            assertNotNull(result.getDocument());
        }

        @Test
        void readFromInputStream() {
            InputStream is = new ByteArrayInputStream(SAMPLE_XML.getBytes(StandardCharsets.UTF_8));
            XmlProcessor result = processor.read(is);
            assertNotNull(result);
            assertNotNull(result.getDocument());
        }

        @Test
        void readFromFile(@TempDir Path tempDir) throws IOException {
            Path xmlFile = tempDir.resolve("test.xml");
            Files.writeString(xmlFile, SAMPLE_XML);

            XmlProcessor result = processor.read(xmlFile.toFile());
            assertNotNull(result);
            assertNotNull(result.getDocument());
        }

        @Test
        void readInvalidXmlThrowsException() {
            assertThrows(XmlProcessorException.class, () ->
                    processor.read("esto no es xml válido"));
        }

        @Test
        void readFromNonExistentFileThrowsException() {
            assertThrows(XmlProcessorException.class, () ->
                    processor.read(new File("/no/existe/archivo.xml")));
        }
    }

    // ── Update ───────────────────────────────────────────────

    @Nested
    class UpdateTests {

        @Test
        void updateSingleNode() {
            String result = processor.read(SAMPLE_XML)
                    .update("/orders/order[@id='1']/status", "OK")
                    .toString();

            assertTrue(result.contains("OK"));
            assertTrue(result.contains("PENDING")); // order 2 sigue PENDING
        }

        @Test
        void updateMultipleNodes() {
            String result = processor.read(SAMPLE_XML)
                    .update("//status", "COMPLETED")
                    .toString();

            assertFalse(result.contains("PENDING"));
            assertTrue(result.contains("COMPLETED"));
        }

        @Test
        void updateNonExistentNodeThrowsException() {
            processor.read(SAMPLE_XML);
            assertThrows(XmlProcessorException.class, () ->
                    processor.update("/orders/nonexistent", "value"));
        }

        @Test
        void updateWithoutReadThrowsException() {
            assertThrows(XmlProcessorException.class, () ->
                    processor.update("/any", "value"));
        }

        @Test
        void updateWithInvalidXPathThrowsException() {
            processor.read(SAMPLE_XML);
            assertThrows(XmlProcessorException.class, () ->
                    processor.update("///[invalid", "value"));
        }
    }

    // ── Delete ───────────────────────────────────────────────

    @Nested
    class DeleteTests {

        @Test
        void deleteSingleNode() {
            String result = processor.read(SAMPLE_XML)
                    .delete("/orders/order[@id='2']")
                    .toString();

            assertTrue(result.contains("Laptop"));
            assertFalse(result.contains("Mouse"));
        }

        @Test
        void deleteMultipleNodes() {
            String result = processor.read(SAMPLE_XML)
                    .delete("//quantity")
                    .toString();

            assertFalse(result.contains("quantity"));
            assertTrue(result.contains("product"));
        }

        @Test
        void deleteNonExistentNodeThrowsException() {
            processor.read(SAMPLE_XML);
            assertThrows(XmlProcessorException.class, () ->
                    processor.delete("/orders/nonexistent"));
        }

        @Test
        void deleteWithoutReadThrowsException() {
            assertThrows(XmlProcessorException.class, () ->
                    processor.delete("/any"));
        }
    }

    // ── Add ──────────────────────────────────────────────────

    @Nested
    class AddTests {

        @Test
        void addElementToSingleParent() {
            String result = processor.read(SAMPLE_XML)
                    .add("/orders/order[@id='1']", "note", "Urgente")
                    .toString();

            assertTrue(result.contains("<note>Urgente</note>"));
        }

        @Test
        void addElementToMultipleParents() {
            String result = processor.read(SAMPLE_XML)
                    .add("//order", "processed", "false")
                    .toString();

            // Debe aparecer dos veces (una por cada order)
            int count = result.split("<processed>false</processed>", -1).length - 1;
            assertEquals(2, count);
        }

        @Test
        void addElementWithNullContent() {
            String result = processor.read(SAMPLE_XML)
                    .add("/orders/order[@id='1']", "emptyTag", null)
                    .toString();

            assertTrue(result.contains("emptyTag"));
        }

        @Test
        void addToNonExistentParentThrowsException() {
            processor.read(SAMPLE_XML);
            assertThrows(XmlProcessorException.class, () ->
                    processor.add("/orders/nonexistent", "child", "value"));
        }

        @Test
        void addWithoutReadThrowsException() {
            assertThrows(XmlProcessorException.class, () ->
                    processor.add("/any", "child", "value"));
        }
    }

    // ── Cadena de operaciones ────────────────────────────────

    @Nested
    class ChainedOperationsTests {

        @Test
        void chainMultipleOperations() {
            String result = processor.read(SAMPLE_XML)
                    .update("/orders/order[@id='1']/status", "OK")
                    .delete("/orders/order[@id='2']")
                    .add("/orders/order[@id='1']", "note", "Procesado")
                    .toString();

            assertTrue(result.contains("OK"));
            assertFalse(result.contains("Mouse"));
            assertTrue(result.contains("<note>Procesado</note>"));
        }

        @Test
        void readReplacesExistingDocument() {
            String otherXml = "<root><item>A</item></root>";

            processor.read(SAMPLE_XML);
            String result = processor.read(otherXml)
                    .update("/root/item", "B")
                    .toString();

            assertTrue(result.contains("B"));
            assertFalse(result.contains("orders"));
        }
    }

    // ── Configuración ────────────────────────────────────────

    @Nested
    class ConfigTests {

        @Test
        void omitXmlDeclaration() {
            XmlProcessorConfig config = new XmlProcessorConfig();
            config.setOmitXmlDeclaration(true);

            String result = new XmlProcessor(config)
                    .read(SAMPLE_XML)
                    .toString();

            assertFalse(result.startsWith("<?xml"));
        }

        @Test
        void includeXmlDeclarationByDefault() {
            String result = processor.read(SAMPLE_XML).toString();
            assertTrue(result.startsWith("<?xml"));
        }
    }

    // ── toString ─────────────────────────────────────────────

    @Nested
    class ToStringTests {

        @Test
        void toStringWithoutReadThrowsException() {
            assertThrows(XmlProcessorException.class, () ->
                    processor.toString());
        }

        @Test
        void toStringPreservesContent() {
            String result = processor.read(SAMPLE_XML).toString();
            assertTrue(result.contains("Laptop"));
            assertTrue(result.contains("Mouse"));
            assertTrue(result.contains("PENDING"));
        }
    }
}
