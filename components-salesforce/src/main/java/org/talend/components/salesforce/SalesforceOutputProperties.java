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
package org.talend.components.salesforce;

import static org.talend.daikon.properties.PropertyFactory.*;
import static org.talend.daikon.properties.presentation.Widget.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.avro.Schema;
import org.talend.components.api.component.Connector;
import org.talend.components.api.component.PropertyPathConnector;
import org.talend.components.api.exception.ComponentException;
import org.talend.components.api.properties.ComponentPropertyFactory;
import org.talend.components.common.SchemaProperties;
import org.talend.daikon.properties.Property;
import org.talend.daikon.properties.ValidationResult;
import org.talend.daikon.properties.presentation.Form;
import org.talend.daikon.properties.presentation.Widget;

public class SalesforceOutputProperties extends SalesforceConnectionModuleProperties {

    public static final String NB_SUCCESS_NAME = "NB_SUCCESS";

    public static final String NB_REJECT_NAME = "NB_REJECT";

    public static final String ACTION_INSERT = "INSERT";

    public static final String ACTION_UPDATE = "UPDATE";

    public static final String ACTION_UPSERT = "UPSERT";

    public static final String ACTION_DELETE = "DELETE";

    public enum OutputAction {
        INSERT,
        UPDATE,
        UPSERT,
        DELETE
    }

    public Property outputAction = newEnum("outputAction", ACTION_INSERT, ACTION_UPDATE, ACTION_UPSERT, ACTION_DELETE); // $NON-NLS-1$

    public Property upsertKeyColumn = newString("upsertKeyColumn"); //$NON-NLS-1$

    //
    // Advanced
    //
    public UpsertRelationTable upsertRelationTable = new UpsertRelationTable("upsertRelationTable");

    //
    // Collections
    //
    protected transient PropertyPathConnector FLOW_CONNECTOR = new PropertyPathConnector(Connector.MAIN_NAME, "schemaFlow");

    protected transient PropertyPathConnector REJECT_CONNECTOR = new PropertyPathConnector(Connector.REJECT_NAME, "schemaReject");

    public SchemaProperties schemaFlow = new SchemaProperties("schemaFlow"); //$NON-NLS-1$

    public SchemaProperties schemaReject = new SchemaProperties("schemaReject"); //$NON-NLS-1$

    public Property NB_SUCCESS;

    public Property NB_REJECT;

    public SalesforceOutputProperties(String name) {
        super(name);
    }

    // Have to use an explicit class to get the override of afterModuleName(), an anonymous
    // class cannot be public and thus cannot be called.
    public class ModuleSubclass extends SalesforceModuleProperties {

        public ModuleSubclass(String name) {
            super(name);
        }

        @Override
        public ValidationResult afterModuleName() throws Exception {
            ValidationResult validationResult = super.afterModuleName();
            List<String> fieldNames = getFieldNames(main.schema);
            upsertKeyColumn.setPossibleValues(fieldNames);
            upsertRelationTable.columnName.setPossibleValues(fieldNames);
            return validationResult;
        }
    }

    public static final boolean POLY = true;

    public void beforeUpsertKeyColumn() {
        upsertKeyColumn.setPossibleValues(getFieldNames(module.main.schema));
    }

    public void beforeUpsertRelationTable() {
        upsertRelationTable.columnName.setPossibleValues(getFieldNames(module.main.schema));
    }

    @Override
    public void setupProperties() {
        super.setupProperties();

        outputAction.setValue(ACTION_INSERT);

        NB_SUCCESS = ComponentPropertyFactory.newReturnProperty(returns, Property.Type.INT, NB_SUCCESS_NAME); //$NON-NLS-1$
        NB_REJECT = ComponentPropertyFactory.newReturnProperty(returns, Property.Type.INT, NB_REJECT_NAME); //$NON-NLS-1$

        module = new ModuleSubclass("module");
        module.connection = connection;
        module.setupProperties();
        upsertRelationTable.setUsePolymorphic(false);
    }

    @Override
    public void setupLayout() {
        super.setupLayout();
        Form mainForm = getForm(Form.MAIN);
        mainForm.addRow(outputAction);
        mainForm.addColumn(upsertKeyColumn);

        Form advancedForm = getForm(Form.ADVANCED);
        advancedForm.addRow(widget(upsertRelationTable).setWidgetType(Widget.WidgetType.TABLE));
        // check
        // I18N
    }

    public void afterOutputAction() {
        refreshLayout(getForm(Form.MAIN));
        refreshLayout(getForm(Form.ADVANCED));
    }

    @Override
    public void refreshLayout(Form form) {
        super.refreshLayout(form);

        if (form.getName().equals(Form.MAIN)) {
            Form advForm = getForm(Form.ADVANCED);
            if (advForm != null) {
                boolean isUpsert = ACTION_UPSERT.equals(outputAction.getValue());
                form.getWidget(upsertKeyColumn.getName()).setHidden(!isUpsert);
                advForm.getWidget(upsertRelationTable.getName()).setHidden(!isUpsert);
            }
        }
    }

    @Override
    protected Set<PropertyPathConnector> getAllSchemaPropertiesConnectors(boolean isOutputConnection) {
        // Need to be implemented by subclass
        throw new ComponentException(new RuntimeException("Need to be implemented!"));
    }

    protected List<String> getFieldNames(Property schema) {
        Schema s = (Schema) schema.getValue();
        List<String> fieldNames = new ArrayList<>();
        for (Schema.Field f : s.getFields()) {
            fieldNames.add(f.name());
        }
        return fieldNames;
    }

}
