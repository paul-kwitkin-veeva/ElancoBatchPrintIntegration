package com.veeva.vps.integration.services;

import com.veeva.vault.vapil.api.VaultClient;
import com.veeva.vault.vapil.api.model.response.QueryResponse;
import com.veeva.vault.vapil.api.request.QueryRequest;
import com.veeva.vps.integration.BatchPrintEngine;
import com.veeva.vps.integration.model.VpsSettingRecord;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class VpsSettingsService {
	static final Logger logger = Logger.getLogger(VpsSettingsService.class);

	public static final String OBJECT_VPS_SETTING = "vps_setting__c";
	public static final String OBJFIELD_EXTERNAL_ID = "external_id__c";
	public static final String OBJFIELD_ID = "id";
	public static final String OBJFIELD_ITEM_DELIMITER = "item_delimiter__c";
	public static final String OBJFIELD_KEY_VALUE_DELIMITER = "key_value_delimiter__c";
	public static final String OBJFIELD_SETTING_DELIMITER = "setting_delimiter__c";
	public static final String OBJFIELD_STATUS = "status__v";
	public static final String OBJFIELD_VALUE = "value__c";
	public static final String STATUS_ACTIVE = "active__v";

	Map<String, VpsSettingRecord> settingRecordMap;

	/**
	 * Helper class to load and retrieve settings records
	 */
	public VpsSettingsService(VaultClient vaultClient) throws Exception {
		super();
		logger.info("VpsSettingHelper - Initialize");
		settingRecordMap = new HashMap<>();
		loadData("",false, vaultClient);
	}

	/**
	 * Helper class to load and retrieve settings records
	 *
	 * @param externalIdFilter external id of the setting record(s) to find
	 * @param useWildCard when true, any records that start with externalIdFilter will be loaded
	 */
	public VpsSettingsService(String externalIdFilter, Boolean useWildCard, VaultClient vaultClient) throws Exception {
		super();
		logger.info("VpsSettingHelper - Initialize");
		settingRecordMap = new HashMap<>();
		loadData(externalIdFilter,useWildCard, vaultClient);
	}

	/**
	 * setting records that have been loaded
	 *
	 * @return map of setting records
	 */
	public Map<String, VpsSettingRecord> items() {return  settingRecordMap;}

	/**
	 * Loads settings records into a map by querying Vault
	 *
	 * @param externalIdFilter external id of the setting record(s) to find
	 * @param useWildCard when true, any records that start with externalIdFilter will be loaded
	 */
	private void loadData(String externalIdFilter, Boolean useWildCard, VaultClient vaultClient) throws Exception {
		logger.info("VpsSettingService.loadData for {" + externalIdFilter + "}; useWildCard = {" + useWildCard + "}");

		try {
			settingRecordMap.clear();

			//query Vault for all settings
			StringBuilder vqlQuery = new StringBuilder();
			vqlQuery.append("select " + OBJFIELD_ID);
			vqlQuery.append("," + OBJFIELD_EXTERNAL_ID);
			vqlQuery.append("," + OBJFIELD_KEY_VALUE_DELIMITER);
			vqlQuery.append("," + OBJFIELD_ITEM_DELIMITER);
			vqlQuery.append("," + OBJFIELD_SETTING_DELIMITER);
			vqlQuery.append("," + OBJFIELD_VALUE);
			vqlQuery.append(" from " + OBJECT_VPS_SETTING);
			vqlQuery.append(" where " + OBJFIELD_STATUS +  " = '" + STATUS_ACTIVE + "' ");

			if ((externalIdFilter != null) && (externalIdFilter.length() > 0)) {
				vqlQuery.append(" and " + OBJFIELD_EXTERNAL_ID + " like '");
				vqlQuery.append(externalIdFilter);
				if (useWildCard) {
					vqlQuery.append("%");
				}
				vqlQuery.append("'");
			}

			QueryResponse queryResponse = vaultClient.newRequest(QueryRequest.class).queryAll(vqlQuery.toString());
			for (QueryResponse.QueryRecord queryResult : queryResponse.getRecords()) {
				String externalId = queryResult.getString(OBJFIELD_EXTERNAL_ID);
				String value = queryResult.getString(OBJFIELD_VALUE);

				VpsSettingRecord settingRecord = new VpsSettingRecord();
				settingRecord.setExternalId(externalId);
				settingRecord.setItemDelimiter(queryResult.getString(OBJFIELD_ITEM_DELIMITER));
				settingRecord.setKeyDelimiter(queryResult.getString(OBJFIELD_KEY_VALUE_DELIMITER));
				settingRecord.setSettingDelimiter(queryResult.getString(OBJFIELD_SETTING_DELIMITER));

				if (value != null) {
					String[] pairs = StringUtils.split(value,settingRecord.getSettingDelimiter());
					for (int i=0;i<pairs.length;i++) {
						String pair = pairs[i];
						String[] keyValue = StringUtils.split(pair,settingRecord.getKeyDelimiter());

						String settingKey = keyValue[0];
						if (keyValue.length > 1) {
							String settingValue = keyValue[1];
							settingRecord.setValue(settingKey, settingValue);
						}
					}
				}
				settingRecordMap.put(externalId, settingRecord);
			}
		}
		catch (Exception exception) {
			logger.error("VpsSettingService.loadData - {" + exception.getMessage() + "}");
			throw exception;
		}
	}
}
