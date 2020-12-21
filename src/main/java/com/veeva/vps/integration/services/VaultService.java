package com.veeva.vps.integration.services;

import com.veeva.vault.vapil.api.VaultClient;
import com.veeva.vault.vapil.api.model.VaultClientId;
import com.veeva.vault.vapil.api.model.response.ObjectRecordActionResponse;
import com.veeva.vault.vapil.api.model.response.ObjectRecordBulkActionResponse;
import com.veeva.vault.vapil.api.model.response.QueryResponse;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.DocumentRenditionRequest;
import com.veeva.vault.vapil.api.request.ObjectLifecycleWorkflowRequest;
import com.veeva.vault.vapil.api.request.QueryRequest;
import com.veeva.vps.integration.BatchPrintEngine;
import com.veeva.vps.integration.model.VpsSettingRecord;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;

public class VaultService {
	static final Logger logger = Logger.getLogger(VaultService.class);

	private String READYFORPRINTSTATE = "ready_for_printing_state__c";
	private String READYFORREPRINTSTATE = "ready_for_reprint_state__c";
	private String PRINTEDUSERACTION = "Objectlifecyclestateuseraction.package_class_instance__c.ready_for_printing_state__c.change_state_to_printed_useraction3__c";
	private String REPRINTEDUSERACTION = "Objectlifecyclestateuseraction.package_class_instance__c.ready_for_reprint_state__c.change_state_to_printed_useraction2__c";
	private String OBJFIELD_DOCUMENT = "document__c";
	private String RELATIONNAME_PCIDOC = "package_class_document_instances__cr";
	private String RELATIONNAME_PCI = "package_class_instance__cr";
	private String OBJECTNAME_PCI = "package_class_instance__c";
	private String RELATIONNAME_PAPERTYPE = "paper_type__cr";
	private String RELATIONNAME_PRINTERTRAY = "printertray__cr";
	private String username = "";
	private String password = "";
	private String domain = "";
	private VaultClient vc;
	//private String pciQuery = "select id, (select id, document__c from package_class_document_instances__cr) from package_class_instance__c where state__v = 'ready_for_printing_state__c'";
	private String pciDocQuery = "select id, document__c, package_class_instance__c, number_of_copies__c, (select name__v from paper_type__cr), (select name__v, printer_name_on_server__c, printing_sides__c from printertray__cr), (select id, state__v from package_class_instance__cr) from package_class_document_instance__c where package_class_instance__c in (select id from package_class_instance__cr where state__v CONTAINS ('ready_for_printing_state__c','ready_for_reprint_state__c'))";
	private String dataKeyDocIds = "docIds";
	private String dataKeyPrinter = "printerTray";
	private String dataKeyPaperSize = "paperSize";
	private String dataKeyNumCopies = "copies";
	private String dataKeyFileLoc = "fileLoc";
	private String dataKeyPCIId = "packageClassInstanceId";
	private String dataKeyPCIState = "packageClassInstanceState";
	private String dataKeyPCIDocId = "packageClassInstanceDocId";
	private String dataKeyPrinterNameOnServer = "packageClassInstanceDocPrinterNameOnServer";
	private String OBJFIELD_NUMCOPIES = "number_of_copies__c";
	private String OBJFIELD_PCI = "package_class_instance__c";
	private String OBJFIELD_SERVER_PRINTER_NAME = "printer_name_on_server__c";
	private String dataKeyPrinterPageSides = "papersides";
	private String OBJFIELD_PAPER_SIDES = "printing_sides__c";


	public VaultService(String username, String password, String domain) {
		this.username = username;
		this.password = password;
		this.domain = domain;

	}

	private VaultClient getVaultClient()
	{
		if (vc == null)
		{
			// Set the Vault Client Id, which is required for all API calls
			VaultClientId vaultClientId = new VaultClientId("veeva","vps","tools",true,"elancoBatchPrint");

			// Instantiate the VAPIL VaultClient using user name and password authentication
			vc = new VaultClient(domain, username, password, vaultClientId);
		}

		return vc;
	}


	public List<Map<String, String>> getDocumentsReadyForPrinting(String tempFolder)
	{
		logger.info("Start getDocumentsReadyForPrinting");
		logger.debug("tempFolder: " + tempFolder);

		List<Map<String, String>> pciDocumentList = getPCIsReadyForPrinting();

		//get all the document bytes
		for (Map<String,String> docData : pciDocumentList) {
			String docIDString = docData.get(dataKeyDocIds);
			int pos = docIDString.indexOf("_");
			File file = new File(tempFolder + "/" + docIDString + ".pdf");
			logger.debug("Temp file: " + file.getAbsolutePath());
			if (!file.exists())
			{
				byte[] curBytes = getViewableRendition(Integer.parseInt(docIDString.substring(0, pos)));
				writeByte(file, curBytes);
			}
			docData.put(dataKeyFileLoc, file.getAbsolutePath());
		}

		logger.info("End getDocumentsReadyForPrinting");

		return pciDocumentList;

	}

	private byte[] getViewableRendition(int docId)
	{
		VaultClient vc = getVaultClient();
		VaultResponse documentResponse = vc.newRequest(DocumentRenditionRequest.class).downloadDocumentRenditionFile(docId, "viewable_rendition__v");
		return documentResponse.getBinaryContent();
	}

	//Get all PCIs in the Ready for Printing State
	private List<Map<String, String>> getPCIsReadyForPrinting()
	{
		logger.info("**************** Start getPCIsReadyForPrinting ***********************");
		List<Map<String, String>> pciDocList = new ArrayList<>();

		VaultClient vc = getVaultClient();
		// Perform a VQL query
		logger.debug("Run query: " + pciDocQuery);
		QueryResponse resp = vc.newRequest(QueryRequest.class)
				.queryAll(pciDocQuery);

		for (QueryResponse.QueryRecord rec : resp.getRecords()) {
			Map<String, String> docData = new HashMap<>();
			String id = rec.getString("id");
			docData.put(dataKeyPCIDocId, id);
			docData.put(dataKeyNumCopies, String.valueOf(rec.getInteger(OBJFIELD_NUMCOPIES)));
			docData.put(dataKeyDocIds, rec.getString(OBJFIELD_DOCUMENT));
			docData.put(dataKeyPCIId, rec.getString(OBJFIELD_PCI));

			QueryResponse response1 = rec.getSubQuery(RELATIONNAME_PAPERTYPE);
			for (QueryResponse.QueryRecord curRec : response1.getRecords()) {
				String papertype = curRec.getString("name__v");
				docData.put(dataKeyPaperSize, papertype);
			}

			QueryResponse response2 = rec.getSubQuery(RELATIONNAME_PRINTERTRAY);
			for (QueryResponse.QueryRecord curRec : response2.getRecords()) {
				String printer = curRec.getString("name__v");
				docData.put(dataKeyPrinter, printer);
				String printerNameOnServer = curRec.getString(OBJFIELD_SERVER_PRINTER_NAME);
				if (printerNameOnServer == null)
					printerNameOnServer = "";

				List<String> paperSides = curRec.getList(OBJFIELD_PAPER_SIDES);
				String paperSidesValue = "";
				if (paperSides != null) {
					if (!paperSides.isEmpty())
					{
						paperSidesValue = paperSides.get(0);
					}
				}

				docData.put(dataKeyPrinterNameOnServer, printerNameOnServer);
				docData.put(dataKeyPrinterPageSides, paperSidesValue);
			}

			QueryResponse response3 = rec.getSubQuery(RELATIONNAME_PCI);
			for (QueryResponse.QueryRecord curRec : response3.getRecords()) {
				String state = curRec.getString("state__v");
				docData.put(dataKeyPCIState, state);
			}


			pciDocList.add(docData);
		}

		logger.info("End getPCIsReadyForPrinting");

		return pciDocList;

	}

	public void movePCIsToPrinted(Set<String> printedPCIIds, Set<String> reprintPCIIds)
	{
		VaultClient vc = getVaultClient();
		if (!printedPCIIds.isEmpty())
		{
			ObjectRecordBulkActionResponse printResponse = vc.newRequest(ObjectLifecycleWorkflowRequest.class).initiateObjectActionOnMultipleRecords(OBJECTNAME_PCI, printedPCIIds, PRINTEDUSERACTION);
			List<VaultResponse.APIResponseError> errors = printResponse.getErrors();
			if (errors != null)
			{
				for (VaultResponse.APIResponseError error : errors) {
					logger.error("Error moving PCI(s) to Printed: " + error.getMessage());
				}
			}
		}

		if (!reprintPCIIds.isEmpty())
		{
			ObjectRecordBulkActionResponse reprintResponse = vc.newRequest(ObjectLifecycleWorkflowRequest.class).initiateObjectActionOnMultipleRecords(OBJECTNAME_PCI, reprintPCIIds, REPRINTEDUSERACTION);
			List<VaultResponse.APIResponseError> errors = reprintResponse.getErrors();
			if (errors != null) {
				for (VaultResponse.APIResponseError error : errors) {
					logger.error("Error moving reprinted PCI(s) to Printed: " + error.getMessage());
				}
			}

		}

	}

	// Method which write the bytes into a file
	private void writeByte(File file, byte[] bytes)
	{
		try {

			// Initialize a pointer
			// in file using OutputStream
			OutputStream os = new FileOutputStream(file);

			// Starts writing the bytes in it
			os.write(bytes);

			// Close the file
			os.close();
		}

		catch (Exception e) {
			System.out.println("Exception: " + e);
		}
	}

	public VpsSettingRecord getVpsSettings(String externalId, Boolean useWildcard) throws Exception {
		VpsSettingsService vpsSettingsService = new VpsSettingsService(externalId, useWildcard, getVaultClient());
		return vpsSettingsService.items().get(externalId);
	}
}
