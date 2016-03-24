package org.talend.components.oracle.runtime;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.IndexedRecord;
import org.talend.daikon.avro.AvroConverter;
import org.talend.daikon.avro.IndexedRecordAdapterFactory;

public class ResultSetAdapterFactory implements IndexedRecordAdapterFactory<ResultSet, IndexedRecord> {

    private Schema                      schema;

    private String                      names[];

    /** The cached AvroConverter objects for the fields of this record. */
    @SuppressWarnings("rawtypes")
    protected transient AvroConverter[] fieldConverter;

    @Override
    public Schema getSchema() {
        return schema;
    }

    @Override
    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    @Override
    public Class<ResultSet> getDatumClass() {
        return ResultSet.class;
    }

    @Override
    public ResultSet convertToDatum(IndexedRecord value) {
        throw new UnmodifiableAdapterException();
    }

    @Override
    public IndexedRecord convertToAvro(ResultSet value) {
        return new ResultSetIndexedRecord(value);
    }

    private class ResultSetIndexedRecord implements IndexedRecord {

        private final ResultSet value;

        public ResultSetIndexedRecord(ResultSet value) {
            this.value = value;
        }

        @Override
        public Schema getSchema() {
            return ResultSetAdapterFactory.this.getSchema();
        }

        @Override
        public void put(int i, Object v) {
            throw new UnmodifiableAdapterException();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object get(int i) {
            if (names == null) {
                names = new String[getSchema().getFields().size()];
                //fieldConverter = new AvroConverter[names.length];
                for (int j = 0; j < names.length; j++) {
                    Field f = getSchema().getFields().get(j);
                    names[j] = f.name();
                    //fieldConverter[j] = SalesforceAvroRegistry.get().getConverterFromString(f);
                }
            }
            
            try {
                return value.getObject((names[i]));
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}