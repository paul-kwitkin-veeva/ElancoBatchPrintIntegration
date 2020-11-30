package com.veeva.vps.integration;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.event.PrintJobAdapter;
import javax.print.event.PrintJobEvent;
import java.awt.print.PrinterJob;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class PrintTest {
    private static boolean jobRunning = true;

    public static void main(String[] args) throws Exception {

        DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
        DocPrintJob printJob = null;

        //DocFlavor flavor = DocFlavor.INPUT_STREAM.PDF;
        PrintRequestAttributeSet attrSet = new HashPrintRequestAttributeSet();

        PrintService[] services = PrintServiceLookup.lookupPrintServices(flavor, attrSet);
        //PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        System.out.println("Number of services1: " + services.length);
        for (PrintService curService : services) {
            String name = curService.getName();
            System.out.println("Service1 name: " + name);
            DocFlavor[] flavors = curService.getSupportedDocFlavors();
            for (DocFlavor curFlavor : flavors) {
                String curFlavorString = curFlavor.toString();
                System.out.println("curFlavorString: " + curFlavorString);
            }
            if (name.equalsIgnoreCase("WUS_OVL_GENPRT0001 (HP Color LaserJet M452nw)"))
            {
                //InputStream is = new BufferedInputStream(new FileInputStream("c:\\Temp\\11089_0_1.pdf"));
                Doc doc = new SimpleDoc(new FileInputStream("c:\\Temp\\11089_0_1.pdf"), flavor, null);
                printJob = curService.createPrintJob();
                printJob.addPrintJobListener(new JobCompleteMonitor());
                //printJob.print(doc, attrSet);
            }
        }

        flavor = DocFlavor.INPUT_STREAM.POSTSCRIPT;
        services = PrintServiceLookup.lookupPrintServices(flavor, attrSet);
        System.out.println("Number of services Postscript: " + services.length);
        for (PrintService curService : services) {
            String name = curService.getName();
            System.out.println("Service Postscript name: " + name);
        }

        flavor = DocFlavor.INPUT_STREAM.PCL;
        services = PrintServiceLookup.lookupPrintServices(flavor, attrSet);
        System.out.println("Number of services PCL: " + services.length);
        for (PrintService curService : services) {
            String name = curService.getName();
            System.out.println("Service PCL name: " + name);
        }

        flavor = DocFlavor.INPUT_STREAM.PDF;
        services = PrintServiceLookup.lookupPrintServices(flavor, attrSet);
        System.out.println("Number of services PDF: " + services.length);
        for (PrintService curService : services) {
            String name = curService.getName();
            System.out.println("Service PDF name: " + name);
        }

        flavor = DocFlavor.INPUT_STREAM.TEXT_PLAIN_HOST;
        services = PrintServiceLookup.lookupPrintServices(flavor, attrSet);
        System.out.println("Number of services TEXT_PLAIN_HOST: " + services.length);
        for (PrintService curService : services) {
            String name = curService.getName();
            System.out.println("Service TEXT_PLAIN_HOST name: " + name);
        }

        flavor = DocFlavor.INPUT_STREAM.TEXT_HTML_US_ASCII;
        services = PrintServiceLookup.lookupPrintServices(flavor, attrSet);
        System.out.println("Number of services TEXT_HTML_US_ASCII: " + services.length);
        for (PrintService curService : services) {
            String name = curService.getName();
            System.out.println("Service TEXT_HTML_US_ASCII name: " + name);
        }

        flavor = DocFlavor.INPUT_STREAM.TEXT_PLAIN_UTF_8;
        services = PrintServiceLookup.lookupPrintServices(flavor, attrSet);
        System.out.println("Number of services TEXT_PLAIN_UTF_8: " + services.length);
        for (PrintService curService : services) {
            String name = curService.getName();
            System.out.println("Service TEXT_PLAIN_UTF_8 name: " + name);
        }

        flavor = DocFlavor.INPUT_STREAM.PNG;
        services = PrintServiceLookup.lookupPrintServices(flavor, attrSet);
        System.out.println("Number of services PNG: " + services.length);
        for (PrintService curService : services) {
            String name = curService.getName();
            System.out.println("Service PNG name: " + name);
        }

        flavor = DocFlavor.BYTE_ARRAY.PDF;
        services = PrintServiceLookup.lookupPrintServices(flavor, attrSet);
        System.out.println("Number of services BYTE_ARRAY: " + services.length);
        for (PrintService curService : services) {
            String name = curService.getName();
            System.out.println("Service BYTE_ARRAY name: " + name);
        }

        flavor = DocFlavor.READER.TEXT_PLAIN;
        services = PrintServiceLookup.lookupPrintServices(flavor, attrSet);
        System.out.println("Number of services READER: " + services.length);
        for (PrintService curService : services) {
            String name = curService.getName();
            System.out.println("Service READER name: " + name);
        }

        flavor = DocFlavor.SERVICE_FORMATTED.PAGEABLE;
        services = PrintServiceLookup.lookupPrintServices(flavor, attrSet);
        System.out.println("Number of services PRINTABLE: " + services.length);

        for (PrintService curService : services) {
            String name = curService.getName();
            System.out.println("Service PRINTABLE name: " + name);

            if (name.equalsIgnoreCase("WUS_OVL_GENPRT0001 (HP Color LaserJet M452nw)"))
            {
//                InputStream is = new BufferedInputStream(new FileInputStream("c:\\Temp\\11089_0_1.pdf"));
//
               // Doc doc = new SimpleDoc(is, flavor, null);
               // printJob = curService.createPrintJob();
                PrinterJob job = PrinterJob.getPrinterJob();
                PDDocument document = PDDocument.load(new File("c:\\Temp\\11089_0_1.pdf"));

                job.setPageable(new PDFPageable(document));
                job.setPrintService(curService);
                //job.print();
            }

            if (name.equalsIgnoreCase("WUS_OVL_XRXPRT003"))
            {
//                InputStream is = new BufferedInputStream(new FileInputStream("c:\\Temp\\11089_0_1.pdf"));
//
                // Doc doc = new SimpleDoc(is, flavor, null);
                // printJob = curService.createPrintJob();
                PrinterJob job = PrinterJob.getPrinterJob();
                PDDocument document = PDDocument.load(new File("c:\\Temp\\11089_0_1.pdf"));

                job.setPageable(new PDFPageable(document));
                job.setPrintService(curService);
                job.print();
            }

        }

        flavor = DocFlavor.URL.PDF;
        services = PrintServiceLookup.lookupPrintServices(flavor, attrSet);
        System.out.println("Number of services URL: " + services.length);
        for (PrintService curService : services) {
            String name = curService.getName();
            System.out.println("Service URL name: " + name);
        }

        flavor = DocFlavor.INPUT_STREAM.PNG;
        services = PrintServiceLookup.lookupPrintServices(flavor, attrSet);
        System.out.println("Number of services PNG: " + services.length);
        for (PrintService curService : services) {
            String name = curService.getName();
            System.out.println("Service PNG name: " + name);
        }
    }

    private static class JobCompleteMonitor extends PrintJobAdapter {
        @Override
        public void printJobCompleted(PrintJobEvent pje) {
            System.out.println("Job completed");
            super.printJobCompleted(pje);
        }

        @Override
        public void printDataTransferCompleted(PrintJobEvent jobEvent) {
            System.out.println("Data Transfer completed");
            super.printDataTransferCompleted(jobEvent);
            jobRunning = false;
        }

        @Override
        public void printJobFailed(PrintJobEvent pje) {
            System.out.println("Job failed");
            super.printJobFailed(pje);
        }

        @Override
        public void printJobCanceled(PrintJobEvent pje) {
            System.out.println("Job canceled");
            super.printJobCanceled(pje);
        }

        @Override
        public void printJobNoMoreEvents(PrintJobEvent pje) {
            System.out.println("Job no more events");
            super.printJobNoMoreEvents(pje);
        }

        @Override
        public void printJobRequiresAttention(PrintJobEvent pje) {
            System.out.println("Job requires attention");
            super.printJobRequiresAttention(pje);
        }
    }
}
