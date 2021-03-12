package com.veeva.vps.integration;

import com.veeva.vps.integration.model.VpsImagePrintable;
import com.veeva.vps.integration.model.VpsSettingRecord;
import com.veeva.vps.integration.services.VaultService;
import com.veeva.vps.integration.services.VpsImageService;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;

import javax.imageio.ImageIO;
import javax.print.*;
import javax.print.attribute.*;
import java.awt.image.BufferedImage;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
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
	private static String dataKeyPrinterPageSides = "papersides";

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
				try
				{
					String curFilePath = docData.get(dataKeyFileLoc);
					if (curFilePath != null) {
						PDDocument document = PDDocument.load(new File(curFilePath));
						DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
						PrintRequestAttributeSet attrSet = new HashPrintRequestAttributeSet();

						PrintService[] services = PrintServiceLookup.lookupPrintServices(flavor, attrSet);

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

							PrinterJob job = PrinterJob.getPrinterJob();

							if (name.equalsIgnoreCase(printerToUse))
							{
								logger.debug("***************** Create print job for doc: " + docData.get(dataKeyDocIds) + " ********************");
								//TODO Add the tray and paper size options to the attributes so that only the printers that support that are listed then match the printer
								logger.debug("paper size: " + docData.get(dataKeyPaperSize));

								attrSet.add(new Copies(Integer.parseInt(docData.get(dataKeyNumCopies))));
								logger.debug("number of copies size: " + docData.get(dataKeyNumCopies));
								String paperSides = docData.get(dataKeyPrinterPageSides);
								if (paperSides.equalsIgnoreCase("duplex__c"))
								{
									attrSet.add(Sides.DUPLEX);
								}

								//Need to determine if this is a zebra printer and therefore need to convert the PDF to an image to print
								List<String> zebraPrinters = settingRecord.getValueAsList(CONFIGURATIONZEBRA);
								if (zebraPrinters.contains(name))
								{
									logger.debug("Print to Zebra Printer");
									//convert the doc to an image
									VpsImageService imageService = new VpsImageService();
									List<String> imageFiles = imageService.convertPDFToImage(document, tempFolder, docData.get(dataKeyPCIDocId));

									for (String imagePath : imageFiles) {
										logger.debug("Print image: " + imagePath);

										Connection thePrinterConn = new TcpConnection(name, TcpConnection.DEFAULT_ZPL_TCP_PORT);

										logger.debug("Connection opened to printer: " + name);
										try {
											// Instantiate connection for ZPL TCP port at given address
											thePrinterConn.open();
											ZebraPrinter printer = ZebraPrinterFactory.getInstance(thePrinterConn);

											int x = 0;
											int y = 0;
											logger.debug("Printing image to zebra printer");
											int numCopies = Integer.parseInt(docData.get(dataKeyNumCopies));
											for (int i = 0; i < numCopies ; i++) {
												printer.printImage(imagePath, x, y);
											}

										} catch (ConnectionException e) {
											e.printStackTrace();
										} catch (ZebraPrinterLanguageUnknownException e) {
											e.printStackTrace();
										} finally {
											// Close the connection to release resources.
											thePrinterConn.close();
										}
									}

								}
								else
								{
									// custom page format
									PageFormat pageFormat = job.getPageFormat(attrSet);
									job.setPageable(new PDFPageable(document));
									job.setPrintService(curService);
									job.print(attrSet);
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

				}
				catch (Exception ex)
				{
					ex.printStackTrace();
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
