package org.talend.components.common;

import static org.talend.daikon.properties.presentation.Widget.*;
import static org.talend.daikon.properties.property.PropertyFactory.*;

import java.util.Collections;
import java.util.Set;

import org.talend.components.api.component.Connector;
import org.talend.components.api.component.ISchemaListener;
import org.talend.components.api.component.PropertyPathConnector;
import org.talend.components.api.properties.ComponentPropertyFactory;
import org.talend.daikon.properties.presentation.Form;
import org.talend.daikon.properties.presentation.Widget;
import org.talend.daikon.properties.property.Property;

public class BulkFileProperties extends FixedConnectorsComponentProperties {

    public Property<String> bulkFilePath = newProperty("bulkFilePath").setRequired();

    public Property<Boolean> append = newBoolean("append");

    public static final String ERROR_MESSAGE_NAME = "ERROR_MESSAGE";

    public Property ERROR_MESSAGE;

    public static final String NB_LINE_NAME = "NB_LINE";

    public Property NB_LINE;

    public ISchemaListener schemaListener;

    public SchemaProperties schema = new SchemaProperties("schema") {

        public void afterSchema() {
            if (schemaListener != null) {
                schemaListener.afterSchema();
            }
        }

    };

    @Override
    public void setupProperties() {
        super.setupProperties();

        ERROR_MESSAGE = ComponentPropertyFactory.newReturnProperty(getReturns(), newString(ERROR_MESSAGE_NAME));
        NB_LINE = ComponentPropertyFactory.newReturnProperty(getReturns(), newInteger(NB_LINE_NAME));
    }

    public void setSchemaListener(ISchemaListener schemaListener) {
        this.schemaListener = schemaListener;
    }

    public BulkFileProperties(String name) {
        super(name);
    }

    @Override
    public void setupLayout() {
        super.setupLayout();
        Form mainForm = new Form(this, Form.MAIN);
        mainForm.addRow(schema.getForm(Form.REFERENCE));
        mainForm.addRow(widget(bulkFilePath).setWidgetType(Widget.FILE_WIDGET_TYPE));
        mainForm.addRow(append);

    }

    @Override
    protected Set<PropertyPathConnector> getAllSchemaPropertiesConnectors(boolean isOutputConnection) {
        if (isOutputConnection) {
            return Collections.emptySet();
        } else {
            return Collections.singleton(new PropertyPathConnector(Connector.MAIN_NAME, "schema"));
        }
    }
}
