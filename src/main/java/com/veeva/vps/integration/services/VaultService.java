package com.veeva.vps.integration.services;

import com.veeva.vault.vapil.api.VaultClient;
import com.veeva.vault.vapil.api.model.VaultClientId;
import com.veeva.vault.vapil.api.model.response.DocumentResponse;
import com.veeva.vault.vapil.api.model.response.QueryResponse;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.DocumentRenditionRequest;
import com.veeva.vault.vapil.api.request.DocumentRequest;
import com.veeva.vault.vapil.api.request.QueryRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VaultService {

	private String READYFORPRINTSTATE = "ready_for_printing_state__c";
	private String PRINTEDUSERACTION = "change_state_to_printed_useraction__c";
	private String OBJFIELD_DOCUMENT = "document__c";
	private String RELATIONNAME_PCIDOC = "package_class_document_instances__cr";
	private String username = "";
	private String password = "";
	private String domain = "";
	private VaultClient vc;
	private String pciQuery = "select id, (select id, document__c from package_class_document_instances__cr) from package_class_instance__c where state__v = 'ready_for_printing_state__c'";
	private String tempFolder = "/Users/paulkwitkin/Desktop/temp";

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


	public List<String> getDocumentsReadyForPrinting()
	{
		List<String> docFileLocations = new ArrayList<String>();
		Map<String, List<String>> pciDocumentMap = getPCIsReadyForPrinting();

		//get all the document bytes
		for (Map.Entry<String, List<String>> pciDocEntry : pciDocumentMap.entrySet()) {
			List<String> docIds = pciDocEntry.getValue();
			for (String docId : docIds) {
				int pos = docId.indexOf("_");
				byte[] curBytes = getViewableRendition(Integer.parseInt(docId.substring(0, pos)));
				File file = new File(tempFolder + "/" + docId + ".pdf");
				writeByte(file, curBytes);
				docFileLocations.add(file.getAbsolutePath());
			}
		}


		return docFileLocations;

	}

	private byte[] getViewableRendition(int docId)
	{
		VaultClient vc = getVaultClient();
		VaultResponse documentResponse = vc.newRequest(DocumentRenditionRequest.class).downloadDocumentRenditionFile(docId, "viewable_rendition__v");
		return documentResponse.getBinaryContent();
	}

	//Get all PCIs in the Ready for Printing State
	private Map<String, List<String>> getPCIsReadyForPrinting()
	{
		Map<String, List<String>> pciDocumentMap = new HashMap<String, List<String>>();

		VaultClient vc = getVaultClient();
		// Perform a VQL query
		QueryResponse resp = vc.newRequest(QueryRequest.class)
				.queryAll(pciQuery);

		for (QueryResponse.QueryRecord rec : resp.getRecords()) {
			String id = rec.getString("id");
			QueryResponse docResponse = rec.getSubQuery(RELATIONNAME_PCIDOC);
			if (pciDocumentMap.containsKey(id))
			{
				List<String> docIds = pciDocumentMap.get(id);
				for (QueryResponse.QueryRecord docRec : docResponse.getRecords()) {
					String docId = docRec.getString(OBJFIELD_DOCUMENT);
					if (!docIds.contains(docId))
						docIds.add(docId);
				}
			}
			else {
				List<String> docIds = new ArrayList<String>();
				for (QueryResponse.QueryRecord docRec : docResponse.getRecords()) {
					String docId = docRec.getString(OBJFIELD_DOCUMENT);
					docIds.add(docId);
				}
				pciDocumentMap.put(id, docIds);
			}
		}

		return pciDocumentMap;

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

}
