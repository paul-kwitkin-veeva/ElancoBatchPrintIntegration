package com.veeva.vps.integration;

import com.veeva.vps.integration.services.VaultService;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import javax.print.event.PrintJobAdapter;
import javax.print.event.PrintJobEvent;

public class BatchPrintEngine {
	static final Logger logger = Logger.getLogger(BatchPrintEngine.class);
	private static boolean jobRunning = true;
	private static String printerToUse = "PDF Printer";

	public static void main(String[] args) throws Exception {
		logger.info("Start com.veeva.vps.integration.BatchPrintEngine");
		//Configure logger
		BasicConfigurator.configure();

		VaultService vaultService = new VaultService("paul.kwitkin@sb-elanco.com", "V33vapass5", "sbelanco-phase-2-sbx.veevavault.com");

		List<String> filePaths = vaultService.getDocumentsReadyForPrinting();

		for (String curFilePath : filePaths) {
			InputStream is = new BufferedInputStream(new FileInputStream(curFilePath));

			DocFlavor flavor = DocFlavor.INPUT_STREAM.PDF;
			PrintRequestAttributeSet attrSet = new HashPrintRequestAttributeSet();
			//PrintService service = PrintServiceLookup.lookupDefaultPrintService();
			PrintService[] services = PrintServiceLookup.lookupPrintServices(flavor, attrSet);
			DocPrintJob printJob = null;
			for (PrintService curService : services) {
				String name = curService.getName();
				if (name.contains(printerToUse))
				{
					printJob = curService.createPrintJob();
				}
			}



			// register a listener to get notified when the job is complete

			printJob.addPrintJobListener(new JobCompleteMonitor());

			// Construct a SimpleDoc with the specified

			// print data, doc flavor and doc attribute set.

			Doc doc = new SimpleDoc(is, flavor, null);
			PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
			//attributes.add(new Destination(new java.net.URI("/Users/paulkwitkin/Desktop/test.ps")));

// Print a document with the specified job attributes.

			printJob.print(doc, attributes);

			while (jobRunning) {

				Thread.sleep(1000);

			}

			logger.info("Exiting app");

			is.close();

			logger.info("End Print Loop");

		}


		logger.info("End com.veeva.vps.integration.BatchPrintEngine");
	}

	private static class JobCompleteMonitor extends PrintJobAdapter {
		@Override
		public void printDataTransferCompleted(PrintJobEvent jobEvent) {
			logger.info("Job completed");
			jobRunning = false;
		}
	}
}
