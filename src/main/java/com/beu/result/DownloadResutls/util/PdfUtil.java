package com.beu.result.DownloadResutls.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class PdfUtil {

    private PdfUtil() {
        // utility class
    }

    /**
     * Converts well-formed XHTML into PDF.
     * IMPORTANT: Input HTML must be strict XHTML.
     */
    public static void htmlToPdf(String xhtml, String outputPath) throws Exception {

        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();

        try (OutputStream os = new FileOutputStream(outputFile)) {

            PdfRendererBuilder builder = new PdfRendererBuilder();

            // Use fast-mode for better performance
            builder.useFastMode();

            // XHTML input (MUST be XML-compliant)
            builder.withHtmlContent(xhtml, null);

            // Output stream
            builder.toStream(os);

            // Build PDF
            builder.run();
        }
    }
}
