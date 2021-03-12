package com.veeva.vps.integration;
import com.veeva.vps.integration.model.VpsImagePrintable;

import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;

import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveryException;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;
import com.zebra.sdk.printer.discovery.NetworkDiscoverer;

public class PrintImage {
    private static String theIpAddress = "10.23.20.119";
    private static String imagePath = "c:\\BPR\\V5T00000001A001-0.png";

    static public void main(String args[]) throws Exception {
        PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
        pras.add(new Copies(1));
        //pras.add(new PrinterResolution(300, 300, PrinterResolution.DPI));
        //PrintService pss[] = PrintServiceLookup.lookupPrintServices(DocFlavor.INPUT_STREAM.AUTOSENSE, pras);

        // Instantiate connection for ZPL TCP port at given address
        Connection thePrinterConn = new TcpConnection(theIpAddress, TcpConnection.DEFAULT_ZPL_TCP_PORT);

        try {
            thePrinterConn.open();
            ZebraPrinter printer = ZebraPrinterFactory.getInstance(thePrinterConn);
            // This example prints "This is a ZPL test." near the top of the label.
            String zplData = "^XA^FO20,20^A0N,25,25^FDThis is a ZPL test.^FS^XZ";

            // Send the data to printer as a byte array.
            //thePrinterConn.write(zplData.getBytes());
            PrinterStatus printerStatus = printer.getCurrentStatus();
            if (printerStatus.isReadyToPrint) {
                System.out.println("Ready To Print");
            } else if (printerStatus.isPaused) {
                System.out.println("Cannot Print because the printer is paused.");
            } else if (printerStatus.isHeadOpen) {
                System.out.println("Cannot Print because the printer head is open.");
            } else if (printerStatus.isPaperOut) {
                System.out.println("Cannot Print because the paper is out.");
            } else {
                System.out.println("Cannot Print.");
            }

            int x = 0;
            int y = 0;
            printer.printImage(imagePath, x, y);

            DiscoveryHandler discoveryHandler = new DiscoveryHandler() {
                List<DiscoveredPrinter> printers = new ArrayList<>();

                public void foundPrinter(DiscoveredPrinter printer) {
                    printers.add(printer);
                }

                public void discoveryFinished() {
                    for (DiscoveredPrinter printer : printers) {
                        System.out.println(printer);
                    }
                    System.out.println("Discovered " + printers.size() + " printers.");
                }

                public void discoveryError(String message) {
                    System.out.println("An error occurred during discovery : " + message);
                }
            };
            try {
                System.out.println("Starting printer discovery.");
                NetworkDiscoverer.findPrinters(discoveryHandler);
            } catch (DiscoveryException e) {
                e.printStackTrace();
            }

        } catch (ConnectionException e) {
            e.printStackTrace();
        } catch (ZebraPrinterLanguageUnknownException e) {
            e.printStackTrace();
        } finally {
            // Close the connection to release resources.
            thePrinterConn.close();
        }


//        PrintService pss[] = PrintServiceLookup.lookupPrintServices(null, null);
//        if (pss.length == 0)
//            throw new RuntimeException("No printer services available.");
//
//        for (PrintService ps : pss) {
//            String name = "ZDesigner ZT420-300dpi ZPL (1)";
//            String curName = ps.getName();
//            if (curName.equalsIgnoreCase(name))
//            {
//                System.out.println("Printing to " + ps);
//                String imagePath = "c:\\BPR\\V5T00000001A001-0.png";
//                BufferedImage myPicture = ImageIO.read(new File(imagePath));
//
//                //DocPrintJob job = ps.createPrintJob();
//                PrinterJob printJob = PrinterJob.getPrinterJob();
//                printJob.setPrintable(new VpsImagePrintable.ImagePrintable(printJob, myPicture));
//                printJob.setPrintService(ps);
//                printJob.print();
//            }
//
//        }


    }

}
