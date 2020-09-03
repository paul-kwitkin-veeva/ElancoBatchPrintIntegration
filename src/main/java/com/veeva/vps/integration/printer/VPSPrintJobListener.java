package com.veeva.vps.integration.printer;

import javax.print.event.PrintJobEvent;
import javax.print.event.PrintJobListener;

public class VPSPrintJobListener implements PrintJobListener
{

	public void printDataTransferCompleted(PrintJobEvent pje)
	{
		System.out.println ("printDataTransferCompleted");
	}

	public void printJobCanceled(PrintJobEvent pje)
	{
		System.out.println ("printJobCanceled");
	}

	public void printJobCompleted(PrintJobEvent pje)
	{
		System.out.println ("printJobCompleted");
	}

	public void printJobFailed(PrintJobEvent pje)
	{
		System.out.println ("printJobFailed");
	}

	public void printJobNoMoreEvents(PrintJobEvent pje)
	{
		System.out.println ("printJobNoMoreEvents");
	}

	public void printJobRequiresAttention(PrintJobEvent pje)
	{
		System.out.println ("printJobRequiresAttention");
	}
}