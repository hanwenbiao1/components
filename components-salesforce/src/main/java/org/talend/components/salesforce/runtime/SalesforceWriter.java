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
package org.talend.components.salesforce.runtime;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sforce.ws.bind.XmlObject;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.talend.components.api.component.runtime.WriteOperation;
import org.talend.components.api.component.runtime.Writer;
import org.talend.components.api.component.runtime.WriterResult;
import org.talend.components.api.container.RuntimeContainer;
import org.talend.components.api.exception.DataRejectException;
import org.talend.components.salesforce.SalesforceOutputProperties;
import org.talend.components.salesforce.tsalesforceoutput.TSalesforceOutputProperties;
import org.talend.daikon.avro.IndexedRecordAdapterFactory;
import org.talend.daikon.avro.util.AvroUtils;

import com.sforce.soap.partner.DeleteResult;
import com.sforce.soap.partner.Error;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.UpsertResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

final class SalesforceWriter implements Writer<WriterResult> {

    private SalesforceWriteOperation salesforceWriteOperation;

    private PartnerConnection connection;

    private String uId;

    private SalesforceSink sink;

    private RuntimeContainer container;

    private TSalesforceOutputProperties sprops;

    private String upsertKeyColumn;

    protected List<String> deleteItems;

    protected List<SObject> insertItems;

    protected List<SObject> upsertItems;

    protected List<SObject> updateItems;

    protected int commitLevel;

    protected boolean exceptionForErrors;

    private int dataCount;

    private int successCount;

    private int rejectCount;

    private transient IndexedRecordAdapterFactory<Object, ? extends IndexedRecord> factory;

    private transient Schema schema;

    public SalesforceWriter(SalesforceWriteOperation salesforceWriteOperation, RuntimeContainer container) {
        this.salesforceWriteOperation = salesforceWriteOperation;
        this.container = container;
        sink = (SalesforceSink) salesforceWriteOperation.getSink();
        sprops = sink.getSalesforceOutputProperties();
        if (sprops.extendInsert.getBooleanValue()) {
            commitLevel = sprops.commitLevel.getIntValue();
        } else {
            commitLevel = 1;
        }
        int arraySize = commitLevel * 2;
        deleteItems = new ArrayList<>(arraySize);
        insertItems = new ArrayList<>(arraySize);
        updateItems = new ArrayList<>(arraySize);
        upsertItems = new ArrayList<>(arraySize);
        upsertKeyColumn = "";
        exceptionForErrors = sprops.ceaseForError.getBooleanValue();

    }

    @Override
    public void open(String uId) throws IOException {
        this.uId = uId;
        connection = sink.connect(container).connection;
        if (null == schema) {
            schema = (Schema) sprops.module.main.schema.getValue();
            if (AvroUtils.isIncludeAllFields(schema)) {
                schema = sink.getSchema(connection, sprops.module.moduleName.getStringValue());
            } // else schema is fully specified
        }
        upsertKeyColumn = sprops.upsertKeyColumn.getStringValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void write(Object datum) throws IOException {
        dataCount++;
        // Ignore empty rows.
        if (null == datum) {
            return;
        }

        // This is all we need to do in order to ensure that we can process the incoming value as an IndexedRecord.
        if (null == factory) {
            factory = (IndexedRecordAdapterFactory<Object, ? extends IndexedRecord>) SalesforceAvroRegistry.get()
                    .createAdapterFactory(datum.getClass());
        }
        IndexedRecord input = factory.convertToAvro(datum);

        if (!TSalesforceOutputProperties.ACTION_DELETE.equals(sprops.outputAction.getValue())) {
            SObject so = new SObject();
            so.setType(sprops.module.moduleName.getStringValue());
            Map<String, Map<String, String>> referenceFieldsMap = null;
            boolean isUpsert = SalesforceOutputProperties.ACTION_UPSERT.equals(sprops.outputAction.getStringValue());
            if (isUpsert) {
                referenceFieldsMap = getReferenceFieldsMap();
            }
            for (Schema.Field f : input.getSchema().getFields()) {
                Object value = input.get(f.pos());
                if (value != null) {
                    Schema.Field se = schema.getField(f.name());
                    if (se != null) {
                        if (isUpsert && referenceFieldsMap != null && referenceFieldsMap.get(se.name()) != null) {
                            Map<String, String> relationMap = referenceFieldsMap.get(se.name());
                            String lookupFieldName = relationMap.get("lookupFieldName");
                            so.setField(lookupFieldName, null);
                            so.getChild(lookupFieldName).setField("type", relationMap.get("lookupFieldModuleName"));
                            addSObjectField(so.getChild(lookupFieldName), se.schema().getType(), relationMap.get("lookupFieldExternalIdName"), value);
                        } else {
                            addSObjectField(so, se.schema().getType(), se.name(), value);
                        }
                    }
                }
            }

            switch (TSalesforceOutputProperties.OutputAction.valueOf(sprops.outputAction.getStringValue())) {
                case INSERT:
                    insert(so);
                    break;
                case UPDATE:
                    update(so);
                    break;
                case UPSERT:
                    upsert(so);
                    break;
                case DELETE:
                    // See below
                    throw new RuntimeException("Impossible");
            }
        } else { // DELETE
            String id = getIdValue(input);
            if (id != null) {
                delete(id);
            }
        }
    }

    protected String getIdValue(IndexedRecord input) {
        String ID = "Id";
        Schema.Field idField = input.getSchema().getField(ID);
        if (null != idField) {
            return (String) input.get(idField.pos());
        }
        throw new RuntimeException(ID + " not found");
    }

    protected void addSObjectField(XmlObject xmlObject, Schema.Type expected, String fieldName, Object value) {
        Object valueToAdd = null;
        // Convert stuff here
        switch (expected) {
            case BYTES:
                valueToAdd = Charset.defaultCharset().decode(ByteBuffer.wrap((byte[]) value)).toString();
                break;
            // case DATE:
            // case DATETIME:
            // valueToAdd = container.formatDate((Date) value, se.getPattern());
            // break;
            default:
                valueToAdd = value;
                break;
        }
        xmlObject.setField(fieldName, valueToAdd);
    }

    protected SaveResult[] insert(SObject sObject) throws IOException {
        insertItems.add(sObject);
        if (insertItems.size() >= commitLevel) {
            return doInsert();
        }
        return null;
    }

    protected SaveResult[] doInsert() throws IOException {
        if (insertItems.size() > 0) {
            SObject[] accs = insertItems.toArray(new SObject[insertItems.size()]);
            String[] changedItemKeys = new String[accs.length];
            SaveResult[] sr;
            try {
                sr = connection.create(accs);
                insertItems.clear();
                if (sr != null && sr.length != 0) {
                    int batch_idx = -1;
                    for (SaveResult result : sr) {
                        handleResults(result.getSuccess(), result.getErrors(), changedItemKeys, ++batch_idx);
                    }
                }
                return sr;
            } catch (ConnectionException e) {
                throw new IOException(e);
            }
        }
        return null;
    }

    protected SaveResult[] update(SObject sObject) throws IOException {
        updateItems.add(sObject);
        if (updateItems.size() >= commitLevel) {
            return doUpdate();
        }
        return null;
    }

    protected SaveResult[] doUpdate() throws IOException {
        if (updateItems.size() > 0) {
            SObject[] upds = updateItems.toArray(new SObject[updateItems.size()]);
            String[] changedItemKeys = new String[upds.length];
            for (int ix = 0; ix < upds.length; ++ix) {
                changedItemKeys[ix] = upds[ix].getId();
            }
            SaveResult[] saveResults;
            try {
                saveResults = connection.update(upds);
                updateItems.clear();
                upds = null;

                if (saveResults != null && saveResults.length != 0) {
                    int batch_idx = -1;
                    for (SaveResult result : saveResults) {
                        handleResults(result.getSuccess(), result.getErrors(), changedItemKeys, ++batch_idx);
                    }
                }
                return saveResults;
            } catch (ConnectionException e) {
                throw new IOException(e);
            }
        }
        return null;
    }

    protected UpsertResult[] upsert(SObject sObject) throws IOException {
        upsertItems.add(sObject);
        if (upsertItems.size() >= commitLevel) {
            return doUpsert();
        }
        return null;
    }

    protected UpsertResult[] doUpsert() throws IOException {
        if (upsertItems.size() > 0) {
            SObject[] upds = upsertItems.toArray(new SObject[upsertItems.size()]);
            String[] changedItemKeys = new String[upds.length];
            for (int ix = 0; ix < upds.length; ++ix) {
                Object value = upds[ix].getField(upsertKeyColumn);
                if (value == null) {
                    changedItemKeys[ix] = "No value for " + upsertKeyColumn + " ";
                } else {
                    changedItemKeys[ix] = upsertKeyColumn;
                }
            }
            UpsertResult[] upsertResults;
            try {
                upsertResults = connection.upsert(upsertKeyColumn, upds);
                upsertItems.clear();
                upds = null;

                if (upsertResults != null && upsertResults.length != 0) {
                    int batch_idx = -1;
                    for (UpsertResult result : upsertResults) {
                        handleResults(result.getSuccess(), result.getErrors(), changedItemKeys, ++batch_idx);
                    }
                }
                return upsertResults;
            } catch (ConnectionException e) {
                new IOException(e);
            }
        }
        return null;

    }

    protected void handleResults(boolean success, Error[] resultErrors, String[] changedItemKeys, int batchIdx)
            throws IOException {
        //StringBuilder errors = new StringBuilder("");

        Map<String, Object> resultMessage = new HashMap<String, Object>();
        StringBuilder errors = new StringBuilder("");
        if (success) {
            successCount++;
            // TODO: send back the ID
        } else {
            //TODO now we use batch mode for commit the data to salesforce, but the batch size is 1 at any time, so the code is ok now, but we need fix it.
            if (exceptionForErrors) {
                errors = SalesforceRuntime.addLog(resultErrors,
                        batchIdx < changedItemKeys.length ? changedItemKeys[batchIdx] : "Batch index out of bounds", null);
            } else {
                rejectCount++;
                if (!sprops.extendInsert.getBooleanValue()) {
                    for (Error error : resultErrors) {
                        if (error.getStatusCode() != null) {
                            resultMessage.put("errorCode", error.getStatusCode().toString());
                        }
                        if (error.getFields() != null) {
                            StringBuffer fields = new StringBuffer();
                            for (String field : error.getFields()) {
                                fields.append(field);
                                fields.append(",");
                            }
                            if (fields.length() > 0) {
                                fields.deleteCharAt(fields.length() - 1);
                            }
                            resultMessage.put("errorFields", fields.toString());
                        }
                        resultMessage.put("errorMessage", error.getMessage());
                    }
                    throw new DataRejectException(resultMessage);
                }
            }
        }
        if (exceptionForErrors && errors.toString().length() > 0) {
            throw new IOException(errors.toString());
        }

    }

    protected DeleteResult[] delete(String id) throws IOException {
        if (id != null) {
            deleteItems.add(id);
            if (deleteItems.size() >= commitLevel) {
                return doDelete();
            }
        }
        return null;
    }

    protected DeleteResult[] doDelete() throws IOException {
        if (deleteItems.size() > 0) {
            String[] delIDs = deleteItems.toArray(new String[deleteItems.size()]);
            String[] changedItemKeys = new String[delIDs.length];
            for (int ix = 0; ix < delIDs.length; ++ix) {
                changedItemKeys[ix] = delIDs[ix];
            }
            DeleteResult[] dr;
            try {
                dr = connection.delete(delIDs);
                deleteItems.clear();

                if (dr != null && dr.length != 0) {
                    int batch_idx = -1;
                    for (DeleteResult result : dr) {
                        handleResults(result.getSuccess(), result.getErrors(), changedItemKeys, ++batch_idx);
                    }
                }
                return dr;
            } catch (ConnectionException e) {
                throw new IOException(e);
            }
        }
        return null;
    }

    @Override
    public WriterResult close() throws IOException {
        logout();
        // this should be computed according to the result of the write I guess but I don't know yet how exceptions are
        // handled by Beam.
        if (container != null) {
            container.setComponentData(container.getCurrentComponentId(), SalesforceOutputProperties.NB_LINE_NAME, dataCount);
            container.setComponentData(container.getCurrentComponentId(), SalesforceOutputProperties.NB_SUCCESS_NAME, successCount);
            container.setComponentData(container.getCurrentComponentId(), SalesforceOutputProperties.NB_REJECT_NAME, rejectCount);
        }
        return new WriterResult(uId, dataCount);
    }

    protected void logout() throws IOException {
        // Finish anything uncommitted
        doInsert();
        doDelete();
        doUpdate();
        doUpsert();
    }

    @Override
    public WriteOperation<WriterResult> getWriteOperation() {
        return salesforceWriteOperation;
    }

    protected Map<String, Map<String, String>> getReferenceFieldsMap() {
        Object value = sprops.upsertRelationTable.columnName.getValue();
        Map<String, Map<String, String>> referenceFieldsMap = null;
        if (value != null && value instanceof List) {
            referenceFieldsMap = new HashMap<>();
            List<String> columns = (List<String>) value;
            List<String> lookupFieldModuleNames = (List<String>) sprops.upsertRelationTable.lookupFieldModuleName.getValue();
            List<String> lookupFieldNames = (List<String>) sprops.upsertRelationTable.lookupFieldName.getValue();
            List<String> externalIdFromLookupFields = (List<String>) sprops.upsertRelationTable.lookupFieldExternalIdName.getValue();
            for (int index = 0; index < columns.size(); index++) {
                Map<String, String> relationMap = new HashMap<>();
                relationMap.put("lookupFieldModuleName", lookupFieldModuleNames.get(index));
                relationMap.put("lookupFieldName", lookupFieldNames.get(index));
                relationMap.put("lookupFieldExternalIdName", externalIdFromLookupFields.get(index));
                referenceFieldsMap.put(columns.get(index), relationMap);
            }
        }
        return referenceFieldsMap;
    }

}
