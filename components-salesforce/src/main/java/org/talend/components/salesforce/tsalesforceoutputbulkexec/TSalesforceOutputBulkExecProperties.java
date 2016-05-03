// ============================================================================
//
// Copyright (C) 2006-2015 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.salesforce.tsalesforceoutputbulkexec;

import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.api.properties.VirtualComponentProperties;
import org.talend.components.salesforce.tsalesforcebulkexec.TSalesforceBulkExecProperties;
import org.talend.components.salesforce.tsalesforceoutputbulk.TSalesforceOutputBulkProperties;
import org.talend.daikon.properties.presentation.Form;

public class TSalesforceOutputBulkExecProperties extends TSalesforceBulkExecProperties implements VirtualComponentProperties {

    public TSalesforceOutputBulkExecProperties(String name) {
        super(name);
    }

    public TSalesforceOutputBulkProperties outputBulkProperties = new TSalesforceOutputBulkProperties("outputBulkProperties");

    @Override
    public void setupLayout() {
        super.setupLayout();
        Form mainForm = getForm(Form.MAIN);
        mainForm.addRow(outputBulkProperties.getForm(Form.REFERENCE));
    }
    
    @Override
    public void refreshLayout(Form form) {
        super.refreshLayout(form);
        
        if(Form.ADVANCED.equals(form.getName())) {
        	boolean isUpsert = ACTION_UPSERT.equals(outputAction.getValue());
        	form.getWidget(upsertRelation.getName()).setHidden(!isUpsert);
        }
    }

    @Override
    public ComponentProperties getInputComponentProperties() {
        outputBulkProperties.schema.schema.setValue(module.main.schema.getValue());
        outputBulkProperties.bulkFilePath.setValue(bulkFilePath.getValue());
        outputBulkProperties.upsertRelation.setValue(upsertRelation.getStoredValue());
        return outputBulkProperties;
    }

    private static final String ADD_QUOTES = "ADD_QUOTES";
    
    @Override
    public ComponentProperties getOutputComponentProperties() {
        TSalesforceBulkExecProperties bulkExecProperties = new TSalesforceBulkExecProperties("bulkExecProperties");
        bulkExecProperties.copyValuesFrom(this);
        
        bulkExecProperties.connection.referencedComponent.componentInstanceId.setTaggedValue(ADD_QUOTES, true);
        bulkExecProperties.module.connection.referencedComponent.componentInstanceId.setTaggedValue(ADD_QUOTES, true);
        
        return bulkExecProperties;
    }
}
