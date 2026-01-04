package com.beu.result.DocumentArchival.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Service provider for rendering HTML/XHTML content into PDF format.
 * <p>
 * This component wraps the 'OpenHTMLtoPDF' engine to provide a standard
 * interface for generating document artifacts from template strings.
 * </p>
 */
@Component
public class DocumentRenderingProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentRenderingProvider.class);

    /**
     * Renders a strict XHTML string into a PDF file at the specified path.
     * * @param xhtmlContent The strictly formatted XML-compliant HTML string.
     * @param outputPath The full file system path where the PDF should be saved.
     */
    public void renderHtmlToPdf(String xhtmlContent, String outputPath) {

        File outputFile = new File(outputPath);

        // Ensure parent directories exist
        if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        try (OutputStream os = new FileOutputStream(outputFile)) {

            PdfRendererBuilder builder = new PdfRendererBuilder();

            // Optimization: Use Fast Mode for quicker rendering
            builder.useFastMode();

            // Load Content
            builder.withHtmlContent(xhtmlContent, null);

            // Output Destination
            builder.toStream(os);

            // Execute Generation
            builder.run();

            LOG.debug("PDF Generated successfully at: {}", outputPath);

        } catch (Exception e) {
            LOG.error("Failed to render PDF document at {}", outputPath, e);
            throw new RuntimeException("Document Rendering Failure", e);
        }
    }
}