package org.talend.components.dataprep;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.talend.components.api.service.ComponentService;
import org.talend.components.api.test.SpringApp;

import javax.inject.Inject;
import java.util.Arrays;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SpringApp.class)
public class TDataSetInputSourceTest {
    @Inject
    private ComponentService componentService;

    private TDataSetInputSource inputSource;

    private TDataSetInputDefinition definition;

    @Before
    public void setInputSource() {
        definition = (TDataSetInputDefinition) (componentService.getComponentDefinition("tDatasetInput"));
        inputSource = (TDataSetInputSource) definition.getRuntime();
    }

    @Test
    public void testCreateReader() throws Exception {
        inputSource.initialize(null, definition.createProperties());
        Assert.assertNotNull(inputSource.createReader(null));
    }

    @Test
    public void testValidateThrowsException() throws Exception {

        TDataSetInputProperties properties = (TDataSetInputProperties) definition.createProperties();
        properties.setValue("url", "http://127.0.0.1");
        inputSource.initialize(null, properties);
        inputSource.validate(null);
    }

    @Test
    public void testGetSchema() throws Exception {
        Assert.assertNull(inputSource.getSchema(null, null));
    }

    @Test
    public void testGetSchemaNames() throws Exception {
        Assert.assertNull(inputSource.getSchemaNames(null));
    }

    @Test
    public void testSplitIntoBundles() throws Exception {
        Assert.assertEquals(Arrays.asList(inputSource), inputSource.splitIntoBundles(0, null));
    }

    @Test
    public void testGetEstimatedSizeBytes() throws Exception {
        Assert.assertTrue(0 == inputSource.getEstimatedSizeBytes(null));
    }

    @Test
    public void testProducesSortedKeys() throws Exception {
        Assert.assertFalse(inputSource.producesSortedKeys(null));
    }
}