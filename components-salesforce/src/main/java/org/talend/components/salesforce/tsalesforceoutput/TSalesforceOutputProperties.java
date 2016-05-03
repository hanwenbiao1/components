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

import static org.talend.daikon.properties.PropertyFactory.*;
import static org.talend.daikon.properties.presentation.Widget.widget;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.SchemaBuilder.FieldAssembler;
import org.apache.avro.SchemaBuilder.FieldBuilder;
import org.apache.avro.SchemaBuilder.RecordBuilder;
import org.talend.components.salesforce.ISchemaListener;
import org.talend.components.salesforce.SalesforceOutputProperties;
import org.talend.daikon.avro.SchemaConstants;
import org.talend.daikon.properties.Property;
import org.talend.daikon.properties.presentation.Form;
import org.talend.daikon.properties.presentation.Widget;
import org.talend.daikon.talend6.Talend6SchemaConstants;

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
                updateOutputSchemas();
                beforeUpsertKeyColumn();
                beforeUpsertRelation();
            }
        });
    }

    private void updateOutputSchemas() {
        // get the main schema (input one)
        Schema inputSchema = (Schema) module.main.schema.getValue();
        if (!extendInsert.getBooleanValue() && retrieveInsertId.getBooleanValue()
                && ACTION_INSERT.equals(outputAction.getValue())) {

            Schema mainOutputSchema = createRecordBuilderFromSchema(inputSchema, "output").name("salesforce_id")
                        .prop(Talend6SchemaConstants.TALEND6_COLUMN_CUSTOM, "true")//$NON-NLS-1$
                        .prop(SchemaConstants.TALEND_IS_LOCKED, "false")//$NON-NLS-1$
                        .prop(SchemaConstants.TALEND_COLUMN_DB_LENGTH, "255")//$NON-NLS-1$
                        .type().stringType().noDefault().endRecord();

            schemaFlow.schema.setValue(mainOutputSchema);
        } else {
            schemaFlow.schema.setValue(inputSchema);
        }

        Schema rejectSchema = createRecordBuilderFromSchema(inputSchema, "rejectOutput").name("errorCode") //$NON-NLS-1$  //$NON-NLS-2$
                .prop(Talend6SchemaConstants.TALEND6_COLUMN_CUSTOM, "true")//$NON-NLS-1$
                // column set as non-read-only, to let the user edit the field if needed
                .prop(SchemaConstants.TALEND_IS_LOCKED, "false")//$NON-NLS-1$
                .prop(SchemaConstants.TALEND_COLUMN_DB_LENGTH, "255")//$NON-NLS-1$
                .type().stringType().noDefault().name("errorFields")//$NON-NLS-1$
                .prop(Talend6SchemaConstants.TALEND6_COLUMN_CUSTOM, "true")//$NON-NLS-1$
                .prop(SchemaConstants.TALEND_IS_LOCKED, "false")//$NON-NLS-1$
                .prop(SchemaConstants.TALEND_COLUMN_DB_LENGTH, "255")//$NON-NLS-1$
                .type().stringType().noDefault().name("errorMessage")//$NON-NLS-1$
                .prop(Talend6SchemaConstants.TALEND6_COLUMN_CUSTOM, "true")//$NON-NLS-1$
                .prop(SchemaConstants.TALEND_IS_LOCKED, "false")//$NON-NLS-1$
                .prop(SchemaConstants.TALEND_COLUMN_DB_LENGTH, "255")//$NON-NLS-1$
                .type().stringType().noDefault().endRecord();

        schemaReject.schema.setValue(rejectSchema);
    }
    
    private FieldAssembler<Schema> createRecordBuilderFromSchema(Schema inputSchema, String newSchemaName) {
        RecordBuilder<Schema> recordBuilder = SchemaBuilder.record(newSchemaName);
        FieldAssembler<Schema> fieldAssembler = recordBuilder.fields();
        for (Field field : inputSchema.getFields()) {
            FieldBuilder<Schema> fieldBuilder = fieldAssembler.name(field.name());
            for (String propName : field.getObjectProps().keySet()) {
                fieldBuilder.prop(propName, field.getObjectProps().get(propName).toString());
            }
            fieldAssembler = fieldBuilder.type().stringType().noDefault();
        }
        return fieldAssembler;
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
        advancedForm.addRow(widget(logFileName).setWidgetType(Widget.WidgetType.FILE));
    }

    public void afterExtendInsert() {
        refreshLayout(getForm(Form.ADVANCED));
        updateOutputSchemas();
    }

    public void afterRetrieveInsertId() {
        refreshLayout(getForm(Form.ADVANCED));
        updateOutputSchemas();
    }

    @Override
    public void refreshLayout(Form form) {
        super.refreshLayout(form);

        if (form.getName().equals(Form.ADVANCED)) {

            form.getChildForm(connection.getName()).getWidget(connection.bulkConnection.getName()).setHidden(true);
            form.getChildForm(connection.getName()).getWidget(connection.httpTraceMessage.getName()).setHidden(true);
            form.getWidget("commitLevel").setHidden(!extendInsert.getBooleanValue());
            form.getWidget("retrieveInsertId").setHidden(
                    extendInsert.getBooleanValue() && ACTION_INSERT.equals(outputAction.getValue()));
            form.getWidget("ignoreNull").setHidden(
                    !ACTION_UPDATE.equals(outputAction.getValue()) || ACTION_UPSERT.equals(outputAction.getValue()));
        }
    }

}
