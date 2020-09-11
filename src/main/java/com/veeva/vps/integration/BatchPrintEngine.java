package com.veeva.vps.integration;

import com.veeva.vps.integration.model.VpsSettingRecord;
import com.veeva.vps.integration.services.VaultService;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import javax.print.attribute.standard.*;
import javax.print.event.PrintJobAdapter;
import javax.print.event.PrintJobEvent;

public class BatchPrintEngine {
	static final Logger logger = Logger.getLogger(BatchPrintEngine.class);
	private static boolean jobRunning = true;
	private static String printerToUse = "PDF Printer";
	private static String dataKeyDocIds = "docIds";
	private static String dataKeyPrinter = "printerTray";
	private static String dataKeyPaperSize = "paperSize";
	private static String dataKeyNumCopies = "copies";
	private static String dataKeyFileLoc = "fileLoc";
	private static String dataKeyPCIId = "packageClassInstanceId";
	private static String dataKeyPCIDocId = "packageClassInstanceDocId";
	private static String dataKeyPCIState = "packageClassInstanceState";
	private static String READYFORPRINTSTATE = "ready_for_printing_state__c";
	private static String READYFORREPRINTSTATE = "ready_for_reprint_state__c";

	public static void main(String[] args) throws Exception {
		logger.info("Start com.veeva.vps.integration.BatchPrintEngine");

		try {
			//Configure logger
			BasicConfigurator.configure();
			// create and load default properties
			Properties defaultProps = new Properties();
			defaultProps.load(BatchPrintEngine.class.getResourceAsStream("/printintegration.properties"));

			String userName = defaultProps.getProperty("username");
			logger.debug("userName: " + userName);
			String password = new String(Base64.getDecoder().decode(defaultProps.getProperty("password")));
			String domain = defaultProps.getProperty("domain");
			logger.debug("domain: " + domain);
			String tempFolder = defaultProps.getProperty("tempFolder");
			logger.debug("tempFolder: " + tempFolder);

			logger.debug("Get Vault Service");
			VaultService vaultService = new VaultService(userName, password, domain);

			//Set up lists of documents successfully printed
			Set<String> docsPrinted = new HashSet<>();
			Set<String> docsRePrinted = new HashSet<>();

			List<Map<String, String>> docList = vaultService.getDocumentsReadyForPrinting(tempFolder);
			logger.debug("Got a document list size: " + docList.size());

			for (Map<String, String> docData : docList) {
				String curFilePath = docData.get(dataKeyFileLoc);
				InputStream is = new BufferedInputStream(new FileInputStream(curFilePath));
				DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
				PrintRequestAttributeSet attrSet = new HashPrintRequestAttributeSet();

				PrintService[] services = PrintServiceLookup.lookupPrintServices(flavor, attrSet);
				logger.debug("Number of services1: " + services.length);
				for (PrintService curService : services) {
					String name = curService.getName();
					logger.debug("Service1 name: " + name);
				}
				VpsSettingRecord settingRecord = vaultService.getVpsSettings("BPRPrinterIntegration", true);
				String printerName = docData.get(dataKeyPrinter);
				String printerToUse = settingRecord.getValue(printerName, "");

				logger.debug("printerName: " + printerName);
				logger.debug("printerToUse: " + printerToUse);



				//attrSet.add(new Destination(new File(tempFolder + "/test.ps").toURI()));
				//attrSet.add(MediaTray.MAIN);
				//PrintService[] services = PrintServiceLookup.lookupPrintServices(flavor, attrSet);
				logger.debug("Number of services: " + services.length);
				DocPrintJob printJob = null;

				for (PrintService curService : services) {
					String name = curService.getName();
					logger.debug("Service name: " + name);
					logger.debug("printerToUse: " + printerToUse);
					if (name.equalsIgnoreCase(printerToUse))
					{
						printJob = curService.createPrintJob();
						printJob.addPrintJobListener(new JobCompleteMonitor());
						Doc doc = new SimpleDoc(is, flavor, null);
						//TODO Add the tray and paper size options to the attributes so that only the printers that support that are listed then match the printer
						logger.debug("paper size: " + docData.get(dataKeyPaperSize));

						if (docData.get(dataKeyPaperSize).equalsIgnoreCase("A4"))
							attrSet.add(MediaSizeName.ISO_A4);
						if (docData.get(dataKeyPaperSize).equalsIgnoreCase("Letter"))
							attrSet.add(MediaSizeName.NA_LETTER);
						if (docData.get(dataKeyPaperSize).equalsIgnoreCase("Legal"))
							attrSet.add(MediaSizeName.NA_LEGAL);
						if (docData.get(dataKeyPaperSize).equalsIgnoreCase("A3"))
							attrSet.add(MediaSizeName.ISO_A3);

						attrSet.add(new Copies(Integer.parseInt(docData.get(dataKeyNumCopies))));
						logger.debug("number of copies size: " + docData.get(dataKeyNumCopies));

						printJob.print(doc, attrSet);

						while (jobRunning) {
							Thread.sleep(1000);
						}

						logger.info("Exiting app");
						is.close();
						logger.info("End Print Loop");

						if (docData.get(dataKeyPCIState).equalsIgnoreCase(READYFORPRINTSTATE))
							docsPrinted.add(docData.get(dataKeyPCIId));
						if (docData.get(dataKeyPCIState).equalsIgnoreCase(READYFORREPRINTSTATE))
							docsRePrinted.add(docData.get(dataKeyPCIId));
					}
				}
			}

			vaultService.movePCIsToPrinted(docsPrinted, docsRePrinted);
		}
		catch (Exception ex)
		{
			logger.error(ex);
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
