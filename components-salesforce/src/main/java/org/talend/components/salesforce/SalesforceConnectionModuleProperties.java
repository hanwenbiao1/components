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

import org.apache.avro.Schema;
import org.talend.components.api.component.Connector;
import org.talend.components.api.component.PropertyPathConnector;
import org.talend.components.api.properties.ComponentPropertyFactory;
import org.talend.components.common.FixedConnectorsComponentProperties;
import org.talend.daikon.properties.Property;
import org.talend.daikon.properties.PropertyFactory;
import org.talend.daikon.properties.presentation.Form;

/**
 * Properties common to input and output Salesforce components.
 */
public abstract class SalesforceConnectionModuleProperties extends FixedConnectorsComponentProperties
        implements SalesforceProvideConnectionProperties {

    // Collections
    //
    public static final String NB_LINE_NAME = "NB_LINE";

    public Property<Integer> NB_LINE = PropertyFactory.newInteger(NB_LINE_NAME);

    public SalesforceConnectionProperties connection = new SalesforceConnectionProperties("connection"); //$NON-NLS-1$

    public SalesforceModuleProperties module;

    protected transient PropertyPathConnector MAIN_CONNECTOR = new PropertyPathConnector(Connector.MAIN_NAME, "module.main");

    public SalesforceConnectionModuleProperties(String name) {
        super(name);
    }

    @Override
    public void setupProperties() {
        super.setupProperties();
        returns = connection.returns;
        NB_LINE = ComponentPropertyFactory.newReturnProperty(returns, NB_LINE);
        // Allow for subclassing
        module = new SalesforceModuleProperties("module");
        module.connection = connection;
    }

    public Schema getSchema() {
        return module.main.schema.getValue();
    }

    @Override
    public void setupLayout() {
        super.setupLayout();
        Form mainForm = new Form(this, Form.MAIN);
        mainForm.addRow(connection.getForm(Form.REFERENCE));
        mainForm.addRow(module.getForm(Form.REFERENCE));

        Form advancedForm = new Form(this, Form.ADVANCED);
        advancedForm.addRow(connection.getForm(Form.ADVANCED));
    }

    @Override
    public SalesforceConnectionProperties getConnectionProperties() {
        return connection;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.daikon.properties.Properties#refreshLayout(org.talend.daikon.properties.presentation.Form)
     */
    @Override
    public void refreshLayout(Form form) {
        super.refreshLayout(form);
        for (Form childForm : connection.getForms()) {
            connection.refreshLayout(childForm);
        }
    }

}
