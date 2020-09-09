package com.veeva.vps.integration;

import java.util.*;

public class Utils {

	public static String appendString(String startString, String endString) {
		StringBuilder sb = new StringBuilder(startString);
		sb.append(endString);
		return sb.toString();
	}

	public static String appendNewLine(String startString, String endString) {
		StringBuilder sb = new StringBuilder(startString);
		sb.append(endString);
		sb.append("\n");
		return sb.toString();
	}

	public static List<String> splitString(String inputString)
	{
		List<String> items = new ArrayList<>();

		if (inputString != null)
		{
			if (!inputString.isEmpty())
			{
				items = Arrays.asList(inputString.split("\\s*,\\s*"));

			}
		}

		return items;
	}
}
