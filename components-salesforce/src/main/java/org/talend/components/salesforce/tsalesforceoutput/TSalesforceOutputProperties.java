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
package org.talend.components.salesforce.tsalesforceoutput;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.SchemaBuilder;
import org.talend.components.salesforce.ISchemaListener;
import org.talend.components.salesforce.SalesforceOutputProperties;
import org.talend.daikon.properties.Property;
import org.talend.daikon.properties.presentation.Form;
import org.talend.daikon.talend6.Talend6SchemaConstants;

import static org.talend.daikon.properties.PropertyFactory.*;

public class TSalesforceOutputProperties extends SalesforceOutputProperties {

    //
    // Advanced
    //
    public Property extendInsert = newBoolean("extendInsert", true); //$NON-NLS-1$

    public Property ceaseForError = newBoolean("ceaseForError", true); //$NON-NLS-1$

    public Property ignoreNull = newBoolean("ignoreNull"); //$NON-NLS-1$

    public Property retrieveInsertId = newBoolean("retrieveInsertId"); //$NON-NLS-1$

    public Property commitLevel = newInteger("commitLevel", 200); //$NON-NLS-1$

    // FIXME - should be file
    public Property logFileName = newString("logFileName"); //$NON-NLS-1$

    public TSalesforceOutputProperties(String name) {
        super(name);
    }

    @Override
    public void setupProperties() {
        super.setupProperties();
        upsertKeyColumn.setType(Property.Type.ENUM);
        
        module.setSchemaListener(new ISchemaListener() {
            
            @Override
            public void afterSchema() {
                // get the main schema (input one)
                Schema inputSchema = (Schema) module.main.schema.getValue();
                if (!extendInsert.getBooleanValue() && retrieveInsertId.getBooleanValue()
                        && ACTION_INSERT.equals(outputAction.getValue())) {
                    
                    // add the salesforce_id column                    
                    Schema s = SchemaBuilder.record("Main")
                            .fields().name("salesforce_id")
                            .prop(Talend6SchemaConstants.TALEND6_COLUMN_CUSTOM, "true")//$NON-NLS-1$
                            .prop(Talend6SchemaConstants.TALEND6_IS_READ_ONLY, "false")//$NON-NLS-1$
                            .prop(Talend6SchemaConstants.TALEND6_COLUMN_TALEND_TYPE, "id_String")//$NON-NLS-1$
                            .prop(Talend6SchemaConstants.TALEND6_COLUMN_LENGTH, "255")//$NON-NLS-1$
                            .type().stringType().noDefault().endRecord();

                    Schema mainOutputSchema = Schema.createUnion(inputSchema, s);
                    schemaFlow.schema.setValue(mainOutputSchema);
                } else {
                    schemaFlow.schema.setValue(inputSchema);
                }

                // setup reject schema output                    
                Schema reject = SchemaBuilder.record("Reject")
                        // record set as read only for talend schema
                        .prop(Talend6SchemaConstants.TALEND6_IS_READ_ONLY, "true")//$NON-NLS-1$
                        .fields().name("errorCode") //$NON-NLS-1$  //$NON-NLS-2$
                        .prop(Talend6SchemaConstants.TALEND6_COLUMN_CUSTOM, "true")//$NON-NLS-1$
                        // column set as non-read-only, to let the user edit the field if needed
                        .prop(Talend6SchemaConstants.TALEND6_IS_READ_ONLY, "false")//$NON-NLS-1$
                        .prop(Talend6SchemaConstants.TALEND6_COLUMN_TALEND_TYPE, "id_String")//$NON-NLS-1$
                        .prop(Talend6SchemaConstants.TALEND6_COLUMN_LENGTH, "255")//$NON-NLS-1$
                        .type().intType().noDefault().name("errorMessage")//$NON-NLS-1$
                        .prop(Talend6SchemaConstants.TALEND6_COLUMN_CUSTOM, "true")//$NON-NLS-1$
                        .prop(Talend6SchemaConstants.TALEND6_IS_READ_ONLY, "false")//$NON-NLS-1$
                        .prop(Talend6SchemaConstants.TALEND6_COLUMN_TALEND_TYPE, "id_String")//$NON-NLS-1$
                        .prop(Talend6SchemaConstants.TALEND6_COLUMN_LENGTH, "255")//$NON-NLS-1$
                        .type().stringType().noDefault().endRecord();

                Schema rejectSchema = Schema.createUnion(inputSchema, reject);
                schemaReject.schema.setValue(rejectSchema);
            }
        });
    }

    @Override
    public void setupLayout() {
        super.setupLayout();

        Form advancedForm = getForm(Form.ADVANCED);
        advancedForm.addRow(extendInsert);
        advancedForm.addRow(ceaseForError);
        advancedForm.addRow(ignoreNull);
        advancedForm.addRow(retrieveInsertId);
        advancedForm.addRow(commitLevel);
        advancedForm.addRow(logFileName);
    }

    public void afterExtendInsert() {
        refreshLayout(getForm(Form.ADVANCED));
    }

    public void afterRetrieveInsertId() {
        refreshLayout(getForm(Form.ADVANCED));
    }

    @Override
    public void refreshLayout(Form form) {
        super.refreshLayout(form);

        if (form.getName().equals(Form.ADVANCED)) {

            form.getChildForm(connection.getName()).getWidget(connection.bulkConnection.getName()).setVisible(false);
            form.getChildForm(connection.getName()).getWidget(connection.httpTraceMessage.getName()).setVisible(false);
            form.getWidget("commitLevel").setVisible(extendInsert.getBooleanValue());
            form.getWidget("retrieveInsertId")
                    .setVisible(!extendInsert.getBooleanValue() && ACTION_INSERT.equals(outputAction.getValue()));
            form.getWidget("ignoreNull")
                    .setVisible(ACTION_UPDATE.equals(outputAction.getValue()) || ACTION_UPSERT.equals(outputAction.getValue()));

        }
    }

}
