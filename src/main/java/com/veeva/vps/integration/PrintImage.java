package com.veeva.vps.integration;
import com.veeva.vps.integration.model.VpsImagePrintable;

import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;
import java.io.File;

import javax.imageio.ImageIO;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;

public class PrintImage {

    static public void main(String args[]) throws Exception {
        PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
        pras.add(new Copies(1));
        //pras.add(new PrinterResolution(300, 300, PrinterResolution.DPI));
        //PrintService pss[] = PrintServiceLookup.lookupPrintServices(DocFlavor.INPUT_STREAM.AUTOSENSE, pras);
        PrintService pss[] = PrintServiceLookup.lookupPrintServices(null, null);
        if (pss.length == 0)
            throw new RuntimeException("No printer services available.");

        for (PrintService ps : pss) {
            String name = "ZDesigner ZT420-300dpi ZPL (1)";
            String curName = ps.getName();
            if (curName.equalsIgnoreCase(name))
            {
                System.out.println("Printing to " + ps);
                String imagePath = "c:\\Temp\\image-0.png";
                BufferedImage myPicture = ImageIO.read(new File(imagePath));

                //DocPrintJob job = ps.createPrintJob();
                PrinterJob printJob = PrinterJob.getPrinterJob();
                printJob.setPrintable(new VpsImagePrintable.ImagePrintable(printJob, myPicture));
                printJob.setPrintService(ps);
                printJob.print();
            }

        }


    }

}
