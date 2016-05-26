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
package org.talend.components.dataprep;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.avro.Schema;
import org.talend.components.api.component.runtime.Sink;
import org.talend.components.api.component.runtime.WriteOperation;
import org.talend.components.api.container.RuntimeContainer;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.dataprep.TDataSetOutputProperties.Mode;
import org.talend.daikon.NamedThing;
import org.talend.daikon.properties.ValidationResult;

public class TDataSetOutputSink implements Sink {

    TDataSetOutputProperties properties;

    @Override
    public WriteOperation<?> createWriteOperation() {
        DataPrepConnectionHandler connectionHandler = new DataPrepConnectionHandler( //
                properties.url.getStringValue(), //
                properties.login.getStringValue(), //
                properties.pass.getStringValue(), //
                properties.dataSetName.getStringValue());
        return new TDataSetWriteOperation(this, connectionHandler, properties.limit.getValue(), properties.mode.getValue());
    }

    @Override
    public void initialize(RuntimeContainer runtimeContainer, ComponentProperties componentProperties) {
        this.properties = (TDataSetOutputProperties) componentProperties;
    }

    @Override
    public ValidationResult validate(RuntimeContainer runtimeContainer) {
        DataPrepConnectionHandler connectionHandler = new DataPrepConnectionHandler(properties.url.getStringValue(),
                properties.login.getStringValue(), properties.pass.getStringValue(), properties.dataSetName.getStringValue());
        if (Mode.LIVE_DATASET.equals(properties.mode.getValue())) {
            return ValidationResult.OK;
        }
        try {
            connectionHandler.validate();
        } catch (IOException e) {
            return new ValidationResult().setStatus(ValidationResult.Result.ERROR).setMessage(e.getMessage());
        }
        return ValidationResult.OK;
    }

    @Override
    public List<NamedThing> getSchemaNames(RuntimeContainer runtimeContainer) throws IOException {
        return Collections.emptyList();
    }

    @Override
    public Schema getSchema(RuntimeContainer runtimeContainer, String s) throws IOException {
        return null;
    }
}