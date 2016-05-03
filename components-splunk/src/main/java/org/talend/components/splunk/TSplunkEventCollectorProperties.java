// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.splunk;

import java.util.Collections;
import java.util.Set;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.SchemaBuilder.FieldAssembler;
import org.apache.avro.SchemaBuilder.FieldBuilder;
import org.talend.components.api.component.Connector;
import org.talend.components.api.component.PropertyPathConnector;
import org.talend.components.api.properties.ComponentPropertyFactory;
import org.talend.components.common.FixedConnectorsComponentProperties;
import org.talend.components.common.SchemaProperties;
import org.talend.components.splunk.objects.SplunkJSONEventField;
import org.talend.daikon.avro.AvroRegistry;
import org.talend.daikon.avro.SchemaConstants;
import org.talend.daikon.avro.util.AvroUtils;
import org.talend.daikon.properties.Property;
import org.talend.daikon.properties.PropertyFactory;
import org.talend.daikon.properties.presentation.Form;

/**
 * The ComponentProperties subclass provided by a component stores the configuration of a component.
 */
public class TSplunkEventCollectorProperties extends FixedConnectorsComponentProperties {

    public static String RESPONSE_CODE_NAME = "RESPONSE_CODE";

    public static String ERROR_MESSAGE_NAME = "ERROR_MESSAGE";

    public Property fullUrl = PropertyFactory.newString("fullUrl", ""); //$NON-NLS-1$

    public SchemaProperties schema = new SchemaProperties("schema"); //$NON-NLS-1$

    public Property token = PropertyFactory.newString("token", "");

    public Property eventsBatchSize = PropertyFactory.newInteger("eventsBatchSize");

    public Property extendedOutput = PropertyFactory.newBoolean("extendedOutput");

    public Property RESPONSE_CODE;

    public Property ERROR_MESSAGE;

    protected transient PropertyPathConnector MAIN_CONNECTOR = new PropertyPathConnector(Connector.MAIN_NAME, "schema");

    public TSplunkEventCollectorProperties(String name) {
        super(name);
        eventsBatchSize.setValue(100);
        extendedOutput.setValue(true);
    }

    @Override
    public void setupLayout() {
        super.setupLayout();
        Form form = new Form(this, Form.MAIN); // $NON-NLS-1$
        form.addRow(schema.getForm(Form.REFERENCE));
        form.addRow(fullUrl);
        form.addRow(token);

        Form advanced = new Form(this, Form.ADVANCED);
        advanced.addRow(extendedOutput);
        advanced.addColumn(eventsBatchSize);
    }

    @Override
    public void setupProperties() {
        super.setupProperties();

        setupDefaultSchema();

        returns = ComponentPropertyFactory.newReturnsProperty();
        RESPONSE_CODE = ComponentPropertyFactory.newReturnProperty(returns, Property.Type.INT, RESPONSE_CODE_NAME); // $NON-NLS-1$
        ERROR_MESSAGE = ComponentPropertyFactory.newReturnProperty(returns, Property.Type.STRING, ERROR_MESSAGE_NAME); // $NON-NLS-1$
    }

    private void setupDefaultSchema() {
        AvroRegistry avroReg = new AvroRegistry();
        FieldAssembler<Schema> record = SchemaBuilder.record("Main").fields();
        for (SplunkJSONEventField metadataField : SplunkJSONEventField.getMetadataFields()) {
            Schema base = avroReg.getConverter(metadataField.getDataType()).getSchema();
            FieldBuilder<Schema> fieldBuilder = record.name(metadataField.getName());
            if (metadataField.getName().equals(SplunkJSONEventField.TIME.getName())) {
                fieldBuilder.prop(SchemaConstants.TALEND_COLUMN_PATTERN, "dd-MM-yyyy");
            }
            fieldBuilder.type(AvroUtils.wrapAsNullable(base)).noDefault();
        }
        Schema defaultSchema = record.endRecord();
        schema.schema.setValue(defaultSchema);
    }

    public int getBatchSize() {
        if (extendedOutput.getBooleanValue()) {
            return eventsBatchSize.getIntValue();
        }
        return 1;
    }

    public void afterExtendedOutput() {
        getForm(Form.ADVANCED).getWidget(eventsBatchSize.getName()).setHidden(!extendedOutput.getBooleanValue());
    }

    public Schema getSchema() {
        return (Schema) schema.schema.getValue();
    }

    @Override
    protected Set<PropertyPathConnector> getAllSchemaPropertiesConnectors(boolean isOutputComponent) {
        if (isOutputComponent) {
            return Collections.singleton(MAIN_CONNECTOR);
        }
        return Collections.emptySet();
    }

}
