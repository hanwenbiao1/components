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

import java.util.HashSet;
import java.util.Set;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.SchemaBuilder.FieldAssembler;
import org.apache.avro.SchemaBuilder.FieldBuilder;
import org.apache.avro.SchemaBuilder.RecordBuilder;
import org.talend.components.api.component.ISchemaListener;
import org.talend.components.api.component.PropertyPathConnector;
import org.talend.components.common.SchemaProperties;
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
    public Property<Boolean> extendInsert = newBoolean("extendInsert", true); //$NON-NLS-1$

    public Property<Boolean> ceaseForError = newBoolean("ceaseForError", true); //$NON-NLS-1$

    public Property<Boolean> ignoreNull = newBoolean("ignoreNull"); //$NON-NLS-1$

    public Property<Boolean> retrieveInsertId = newBoolean("retrieveInsertId"); //$NON-NLS-1$

    public Property<Integer> commitLevel = newInteger("commitLevel", 200); //$NON-NLS-1$

    public Property<String> logFileName = newString("logFileName"); //$NON-NLS-1$

    public TSalesforceOutputProperties(String name) {
        super(name);
    }

    @Override
    public void setupProperties() {
        super.setupProperties();
        // TODO, chack why this was made an ENUM
        // upsertKeyColumn.setType(Property.Type.ENUM);

        module.setSchemaListener(new ISchemaListener() {

            @Override
            public void afterSchema() {
                updateOutputSchemas();
                beforeUpsertKeyColumn();
                beforeUpsertRelationTable();
            }
        });
    }

    private void updateOutputSchemas() {
        // get the main schema (input one)
        Schema inputSchema = module.main.schema.getValue();
        if (!extendInsert.getValue() && retrieveInsertId.getValue() && OutputAction.INSERT.equals(outputAction.getValue())) {

            Schema mainOutputSchema = createRecordBuilderFromSchema(inputSchema, "output").name("salesforce_id")
                    .prop(Talend6SchemaConstants.TALEND6_COLUMN_CUSTOM, "true")//$NON-NLS-1$
                    .prop(SchemaConstants.TALEND_IS_LOCKED, "false")//$NON-NLS-1$
                    .prop(SchemaConstants.TALEND_COLUMN_DB_LENGTH, "255")//$NON-NLS-1$
                    .type().stringType().noDefault().endRecord();

            schemaFlow.schema.setValue(mainOutputSchema);
        } else {
            schemaFlow.schema.setValue(inputSchema);
        }

        if (!extendInsert.getValue() && !ceaseForError.getValue()) {
            Schema rejectSchema = createRecordBuilderFromSchema(inputSchema, "rejectOutput").name("errorCode") //$NON-NLS-1$ //$NON-NLS-2$
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
        } else {
            schemaReject.schema.setValue(SchemaProperties.EMPTY_SCHEMA);
        }
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

    public void afterCeaseForError() {
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
            form.getWidget("commitLevel").setHidden(!extendInsert.getValue());
            form.getWidget("retrieveInsertId")
                    .setHidden(extendInsert.getValue() && OutputAction.INSERT.equals(outputAction.getValue()));
            form.getWidget("ignoreNull").setHidden(
                    !OutputAction.UPDATE.equals(outputAction.getValue()) || OutputAction.UPSERT.equals(outputAction.getValue()));
        }
    }

    @Override
    protected Set<PropertyPathConnector> getAllSchemaPropertiesConnectors(boolean isOutputConnection) {
        HashSet<PropertyPathConnector> connectors = new HashSet<>();
        if (isOutputConnection) {
            connectors.add(FLOW_CONNECTOR);
            if (!extendInsert.getValue() && !ceaseForError.getValue()) {
                connectors.add(REJECT_CONNECTOR);
            }
        } else {
            connectors.add(MAIN_CONNECTOR);
        }
        return connectors;
    }

}
