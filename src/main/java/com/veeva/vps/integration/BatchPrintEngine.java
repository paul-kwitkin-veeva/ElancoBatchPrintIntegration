package com.veeva.vps.integration;

import com.veeva.vps.integration.model.VpsImagePrintable;
import com.veeva.vps.integration.model.VpsSettingRecord;
import com.veeva.vps.integration.services.VaultService;
import com.veeva.vps.integration.services.VpsImageService;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

import javax.imageio.ImageIO;
import javax.print.*;
import javax.print.attribute.*;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;
import java.io.*;
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
	private static String CONFIGURATIONZEBRA = "Zebra_Printers";
	private static String dataKeyPrinterNameOnServer = "packageClassInstanceDocPrinterNameOnServer";

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
			try {
				//clear the temp folder
				FileUtils.cleanDirectory(new File(tempFolder));
			}
			catch (IOException ioException)
			{
				//logger.error(ioException);
			}

			logger.debug("Get Vault Service");
			VaultService vaultService = new VaultService(userName, password, domain);

			//Set up lists of documents successfully printed
			Set<String> docsPrinted = new HashSet<>();
			Set<String> docsRePrinted = new HashSet<>();

			List<Map<String, String>> docList = vaultService.getDocumentsReadyForPrinting(tempFolder);
			logger.debug("Got a document list size: " + docList.size());

			for (Map<String, String> docData : docList) {
				String curFilePath = docData.get(dataKeyFileLoc);
				PDDocument document = PDDocument.load(new File(curFilePath));
				DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
				PrintRequestAttributeSet attrSet = new HashPrintRequestAttributeSet();

				PrintService[] services = PrintServiceLookup.lookupPrintServices(flavor, attrSet);
//				logger.debug("Number of services1: " + services.length);
//				for (PrintService curService : services) {
//					String name = curService.getName();
//					logger.debug("Service1 name: " + name);
//				}
				VpsSettingRecord settingRecord = vaultService.getVpsSettings("BPRPrinterIntegration", true);
				String printerName = docData.get(dataKeyPrinter);
				if (printerName == null)
					printerName = "";

				String printerToUse = settingRecord.getValue(printerName, "");
				if (printerToUse.isEmpty())
				{
					printerToUse = docData.get(dataKeyPrinterNameOnServer);
				}

				logger.debug("printerName: " + printerName);
				logger.debug("printerToUse: " + printerToUse);
				logger.debug("Number of services: " + services.length);
				DocPrintJob printJob = null;

				for (PrintService curService : services) {
					String name = curService.getName();
					logger.debug("Service name: " + name);
					logger.debug("printerToUse: " + printerToUse);

//					printFunctionality(curService, "Trays", MediaTray.class);
//					printFunctionality(curService, "Copies", Copies.class);
//					printFunctionality(curService, "Print Quality", PrintQuality.class);
//					printFunctionality(curService, "Color", ColorSupported.class);
//					printFunctionality(curService, "Media Size", MediaSize.class);
//					printFunctionality(curService, "Accepting Jobs", PrinterIsAcceptingJobs.class);
					PrinterJob job = PrinterJob.getPrinterJob();

					if (name.equalsIgnoreCase(printerToUse))
					{
						logger.debug("***************** Create print job for doc: " + docData.get(dataKeyDocIds) + " ********************");
						//TODO Add the tray and paper size options to the attributes so that only the printers that support that are listed then match the printer
						logger.debug("paper size: " + docData.get(dataKeyPaperSize));
//
//						if (docData.get(dataKeyPaperSize).equalsIgnoreCase("A4"))
//							attrSet.add(MediaSizeName.ISO_A4);
//						if (docData.get(dataKeyPaperSize).equalsIgnoreCase("Letter"))
//							attrSet.add(MediaSizeName.NA_LETTER);
//						if (docData.get(dataKeyPaperSize).equalsIgnoreCase("Legal"))
//							attrSet.add(MediaSizeName.NA_LEGAL);
//						if (docData.get(dataKeyPaperSize).equalsIgnoreCase("A3"))
//							attrSet.add(MediaSizeName.ISO_A3);

						attrSet.add(new Copies(Integer.parseInt(docData.get(dataKeyNumCopies))));
						logger.debug("number of copies size: " + docData.get(dataKeyNumCopies));

						//Need to determine if this is a zebra printer and therefore need to convert the PDF to an image to print
						List<String> zebraPrinters = settingRecord.getValueAsList(CONFIGURATIONZEBRA);
						if (zebraPrinters.contains(name))
						{
							//convert the doc to an image
							VpsImageService imageService = new VpsImageService();
							List<String> imageFiles = imageService.convertPDFToImage(document, tempFolder, docData.get(dataKeyPCIDocId));

							for (String imagePath : imageFiles) {
								//set the image to be printed
								//PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
								attrSet.add(new PrinterResolution(300, 300, PrinterResolution.DPI));

								BufferedImage myPicture = ImageIO.read(new File(imagePath));

								job.setPrintable(new VpsImagePrintable.ImagePrintable(job, myPicture));
								job.setPrintService(curService);
								job.print(attrSet);
							}
						}
						else
						{
							job.setPageable(new PDFPageable(document));
							job.setPrintService(curService);
							job.print();
						}

						logger.info("Exiting app");
						document.close();
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

	private static void printFunctionality(PrintService serv, String attrName, Class<? extends Attribute> attr) {
		boolean isSupported = serv.isAttributeCategorySupported(attr);
		System.out.println("    " + attrName + ": " + (isSupported ? "Y" : "N"));
	}

	private static class JobCompleteMonitor extends PrintJobAdapter {
		@Override
		public void printJobCompleted(PrintJobEvent pje) {
			logger.info("Job completed");
			super.printJobCompleted(pje);
		}

		@Override
		public void printDataTransferCompleted(PrintJobEvent jobEvent) {
			logger.info("Data Transfer completed");
			super.printDataTransferCompleted(jobEvent);
			jobRunning = false;
		}

		@Override
		public void printJobFailed(PrintJobEvent pje) {
			logger.info("Job failed");
			super.printJobFailed(pje);
		}

		@Override
		public void printJobCanceled(PrintJobEvent pje) {
			logger.info("Job canceled");
			super.printJobCanceled(pje);
		}

		@Override
		public void printJobNoMoreEvents(PrintJobEvent pje) {
			logger.info("Job no more events");
			super.printJobNoMoreEvents(pje);
		}

		@Override
		public void printJobRequiresAttention(PrintJobEvent pje) {
			logger.info("Job requires attention");
			super.printJobRequiresAttention(pje);
		}
	}
}
