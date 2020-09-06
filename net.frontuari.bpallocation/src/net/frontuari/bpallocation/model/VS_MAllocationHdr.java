/**
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Copyright (C) 2020 Ing. Victor Suarez <victor.suarez.is@gmail.com> and contributors (see README.md file).
 */
package net.frontuari.bpallocation.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_C_Invoice;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MDocType;
import org.compiere.model.MPeriod;
import org.compiere.model.MSysConfig;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.Query;
import org.compiere.model.X_C_Invoice;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;

/**
 * Extension of {@link MAllocationHdr} for customize without modify the trunk.
 * 
 * @author <a href="mailto:victor.suarez.is@gmail.com">Ing. Victor Suarez</a>
 *
 */
public class VS_MAllocationHdr extends MAllocationHdr implements DocAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6907997672179821140L;

	/**
	 * @param ctx
	 * @param C_AllocationHdr_ID
	 * @param trxName
	 */
	public VS_MAllocationHdr(Properties ctx, int C_AllocationHdr_ID, String trxName) {
		super(ctx, C_AllocationHdr_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param ctx
	 * @param IsManual
	 * @param DateTrx
	 * @param C_Currency_ID
	 * @param description
	 * @param trxName
	 */
	public VS_MAllocationHdr(Properties ctx, boolean IsManual, Timestamp DateTrx, int C_Currency_ID, String description,
			String trxName) {
		super(ctx, IsManual, DateTrx, C_Currency_ID, description, trxName);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public VS_MAllocationHdr(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}
	
	/**	Lines						*/
	private MAllocationLine[]	m_lines = null;
	
	/**	Process Message 			*/
	public String		processMsg = null;
	
	/**	Logger						*/
	private static CLogger log = CLogger.getCLogger(MAllocationHdr.class);
	
	public boolean isReversal() {
		return getReversal_ID() > 0;
	}

	@Override
	public String prepareIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());
		processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		if (processMsg != null)
			return DocAction.STATUS_Invalid;

		//	Std Period open?
		MPeriod.testPeriodOpen(getCtx(), getDateAcct(), MDocType.DOCBASETYPE_PaymentAllocation, getAD_Org_ID());
		
		m_lines = getLines(true);
		if (m_lines.length == 0)
		{
			processMsg = "@NoLines@";
			return DocAction.STATUS_Invalid;
		}
		
		// Stop the Document Workflow if invoice to allocate is as paid
		if (!isReversal()) {
			for (MAllocationLine line :m_lines)
			{	
				if (line.getC_Invoice_ID() != 0)
				{
					StringBuilder whereClause = new StringBuilder(I_C_Invoice.COLUMNNAME_C_Invoice_ID).append("=? AND ") 
									   .append(I_C_Invoice.COLUMNNAME_IsPaid).append("=? AND ")
									   .append(I_C_Invoice.COLUMNNAME_DocStatus).append(" NOT IN (?,?)");
					boolean InvoiceIsPaid = new Query(getCtx(), I_C_Invoice.Table_Name, whereClause.toString(), get_TrxName())
					.setClient_ID()
					.setParameters(line.getC_Invoice_ID(), "Y", X_C_Invoice.DOCSTATUS_Voided, X_C_Invoice.DOCSTATUS_Reversed)
					.match();
					if (InvoiceIsPaid && line.getAmount().signum() > 0)
						throw new  AdempiereException("@ValidationError@ @C_Invoice_ID@ @IsPaid@");
				}
			}	
		}
		
		//	Add up Amounts & validate
		int daysDiffPermitted = MSysConfig.getIntValue("VS_DaysDiffPermittedForAllocation", 0, getAD_Client_ID());
		BigDecimal approval = Env.ZERO;
		for (int i = 0; i < m_lines.length; i++)
		{
			MAllocationLine line = m_lines[i];
			approval = approval.add(line.getWriteOffAmt()).add(line.getDiscountAmt());
			//	Make sure there is BP
			if (line.getC_BPartner_ID() == 0)
			{
				processMsg = "No Business Partner";
				return DocAction.STATUS_Invalid;
			}

			// IDEMPIERE-1850 - validate date against related docs
				// https://github.com/victorsuarezis/dev.vsuarez.allocation/issues/1
			if (line.getC_Invoice_ID() > 0) {
				int daysDiff = TimeUtil.getDaysBetween(line.getC_Invoice().getDateAcct(), getDateAcct());
				if (daysDiff < daysDiffPermitted) {
					String msg = Msg.getMsg(getCtx(), "VS_WrongAllocationDate");
					log.log(Level.SEVERE, msg);
					setProcessMsg(msg);
					return DocAction.STATUS_Invalid;
				}
			}
			if (line.getC_Payment_ID() > 0) {
				int daysDiff = TimeUtil.getDaysBetween(line.getC_Payment().getDateAcct(), getDateAcct());
				if (daysDiff < daysDiffPermitted) {
					String msg = Msg.getMsg(getCtx(), "VS_WrongAllocationDate");
					log.log(Level.SEVERE, msg);
					setProcessMsg(msg);
					return DocAction.STATUS_Invalid;
				}
			}
		}
		setApprovalAmt(approval);
		//
		processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
		if (processMsg != null)
			return DocAction.STATUS_Invalid;
		
		if (!DOCACTION_Complete.equals(getDocAction()))
			setDocAction(DOCACTION_Complete);
		
		return DocAction.STATUS_InProgress;
	}

	private void setProcessMsg(String msg) {
		processMsg = msg;
	}
	
	public String getProcessMsg() {
		return processMsg;
	}

}
