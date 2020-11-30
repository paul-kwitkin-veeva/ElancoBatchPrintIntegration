package com.veeva.vps.integration.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VpsImageService {

    public List<String> convertPDFToImage(PDDocument document, String outputDir, String filePrepend) throws IOException {
        List<String> imageFiles = new ArrayList<>();
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        for (int page = 0; page < document.getNumberOfPages(); ++page)
        {
            BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.BINARY);
            String fileName = outputDir + "/" + filePrepend + "-" + page + ".png";
            ImageIOUtil.writeImage(bim, fileName, 300);
            imageFiles.add(fileName);
        }
        document.close();
        return imageFiles;
    }
}
