/*
 * --------------------------------------------------------------------
 * UDC:         vps_setting_record__c
 * Author:      markarnold @ Veeva
 * Date:        2019-07-25
 * --------------------------------------------------------------------
 * Description: Vps Setting Record
 * --------------------------------------------------------------------
 * Copyright (c) 2019 Veeva Systems Inc.  All Rights Reserved.
 * This code is based on pre-existing content developed and
 * owned by Veeva Systems Inc. and may only be used in connection
 * with the deliverable with which it was provided to Customer.
 * --------------------------------------------------------------------
 */
package com.veeva.vps.integration.model;

import org.apache.commons.lang.StringUtils;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

public class VpsSettingRecord {

	public static final String VPS_SETTING_ENABLED = "enabled";
	public static final String VPS_SETTING_EXCLUDED_USERS = "excluded_users";
	public static final String VPS_SETTING_INCLUDED_USERS = "included_users";
	public static final String VPS_SETTING_INCLUDE_ALL_USERS = "ALL";

	private String externalId;
	private String itemDelimiter;
	private String keyDelimiter;
	private String settingDelimiter;
	private Map<String,String> valueMap;

	/**
	 * Class to store setting records
	 */
	public VpsSettingRecord() {
		super();
		valueMap = new HashMap<>();
	}

	/**
	 * @return Set of userids to be excluded from SDK Component execution
	 */
	public Set<String> getExcludedUsers() {
		return getValueAsSet(VPS_SETTING_EXCLUDED_USERS);
	}

	/**
	 *
	 * @return external id of setting record
	 */
	public String getExternalId() {
		return externalId;
	}

	/**
	 *
	 * @return Set of userids to be included in SDK Component execution
	 */
	public Set<String> getIncludedUsers() {
		return getValueAsSet(VPS_SETTING_INCLUDED_USERS);
	}

	/**
	 *
	 * @return delimiter for items in a set/list
	 */
	public String getItemDelimiter() {
		return itemDelimiter;
	}

	/**
	 *
	 * @return delimiter for kay/value pair
	 */
	public String getKeyDelimiter() {
		return keyDelimiter;
	}

	/**
	 *
	 * @return delimiter for multiple settings in one record
	 */
	public String getSettingDelimiter() {
		return settingDelimiter;
	}

	/**
	 *
	 * @param key name of setting to retrieve
	 * @param nullValue value to return of setting is missing or null
	 * @return value of setting
	 */
	public String getValue(String key, String nullValue) {
		String returnValue = nullValue;
		if (valueMap.containsKey(key)) {
			String stringValue = valueMap.get(key);
			if (stringValue != null) {
				returnValue = valueMap.get(key);
			}
		}

		return returnValue;
	}

	/**
	 *
	 * @param key name of setting to retrieve
	 * @param nullValue value to return of setting is missing or null
	 * @return value of setting
	 */
	public Boolean getValueAsBoolean(String key, Boolean nullValue) {
		Boolean returnValue = nullValue;
		if (valueMap.containsKey(key)) {
			String stringValue = valueMap.get(key);
			if (stringValue != null) {
				returnValue = valueMap.get(key).toLowerCase().equals("true");
			}
		}
		return returnValue;
	}

	/**
	 *
	 * @param key name of setting to retrieve
	 * @param nullValue value to return of setting is missing or null
	 * @return value of setting
	 */
	public LocalDate getValueAsDate(String key, LocalDate nullValue) {
		LocalDate returnValue = nullValue;
		if (valueMap.containsKey(key)) {
			String stringValue = valueMap.get(key);
			if (stringValue != null) {
				returnValue = LocalDate.parse(stringValue);
			}
		}
		return returnValue;
	}

	/**
	 *
	 * @param key name of setting to retrieve
	 * @param nullValue value to return of setting is missing or null
	 * @return value of setting
	 */
	public ZonedDateTime getValueAsDateTime(String key, ZonedDateTime nullValue) {
		ZonedDateTime returnValue = nullValue;
		if (valueMap.containsKey(key)) {
			String stringValue = valueMap.get(key);
			if (stringValue != null) {
				returnValue = ZonedDateTime.parse(stringValue);
			}
		}
		return returnValue;
	}

	/**
	 *
	 * @param key name of setting to retrieve
	 * @param nullValue value to return of setting is missing or null
	 * @return value of setting
	 */
	public Integer getValueAsInteger(String key, Integer nullValue) {
		Integer returnValue = nullValue;
		if (valueMap.containsKey(key)) {
			String stringValue = valueMap.get(key);
			if (stringValue != null) {
				returnValue = Integer.valueOf(valueMap.get(key));
			}
		}
		return returnValue;
	}

	/**
	 *
	 * @param key name of setting to retrieve
	 * @return list of values; empty list if setting is missing
	 */
	public List<String> getValueAsList(String key) {
		List<String> resultList = new ArrayList<>();
		if (valueMap.containsKey(key)) {
			String stringValue = valueMap.get(key);
			if (stringValue != null) {
				String[] items = StringUtils.split(stringValue,getItemDelimiter());
				for (int i=0;i<items.length;i++) {
					String itemValue = items[i];
					resultList.add(itemValue);
				}
			}
		}
		return resultList;
	}

	/**
	 *
	 * @param key name of setting to retrieve
	 * @return set of values; empty set if setting is missing
	 */
	public Set<String> getValueAsSet(String key) {
		Set<String> resultSet = new HashSet<>();
		if (valueMap.containsKey(key)) {
			String stringValue = valueMap.get(key);
			if (stringValue != null) {
				String[] items = StringUtils.split(stringValue,getItemDelimiter());
				for (int i=0;i<items.length;i++) {
					String itemValue = items[i];
					resultSet.add(itemValue);
				}
			}
		}
		return resultSet;
	}

	/**
	 *
	 * @return boolean indicating whether the component is enabled
	 */
	public Boolean isEnabled() {
		return getValueAsBoolean(VPS_SETTING_ENABLED,false);
	}

	/**
	 *
	 * @return boolean indicating whether the component is enabled for the current user
	 */
	public Boolean isEnabledForCurrentUser(String currentUserId) {
		Boolean result = false;
		if (isEnabled()) {
			//make sure the current user isn't excluded
			if (!getExcludedUsers().contains(currentUserId)) {
				Set<String> includedUsers = getIncludedUsers();

				//make sure all users are included or the current user is included
				if ((includedUsers.contains(VPS_SETTING_INCLUDE_ALL_USERS)) || (includedUsers.contains(currentUserId))) {
					result = true;
				}
			}
		}
		return result;
	}

	/**
	 *
	 * @param value value of external id
	 */
	public void setExternalId(String value) {
		externalId = value;
	}

	/**
	 *
	 * @param value value of item delimiter
	 */
	public void setItemDelimiter(String value) {
		itemDelimiter = value;
		if (itemDelimiter != null) {
			itemDelimiter = itemDelimiter.replace("LF","\n");
		}
	}

	/**
	 *
	 * @param value value of key delimiter
	 */
	public void setKeyDelimiter(String value) {
		keyDelimiter = value;
		if (keyDelimiter != null) {
			keyDelimiter = keyDelimiter.replace("LF","\n");
		}
	}

	/**
	 *
	 * @param value value of setting delimiter
	 */
	public void setSettingDelimiter(String value) {
		settingDelimiter = value;
		if (settingDelimiter != null) {
			settingDelimiter = settingDelimiter.replace("LF","\n");
		}
	}

	/**
	 *
	 * @param key name of setting
	 * @param value value of setting
	 */
	public void setValue(String key, String value) {
		valueMap.put(key,value);
	}

}