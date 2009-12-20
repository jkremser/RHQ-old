package org.rhq.core.clientapi.agent.metadata.test;

import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor.Relationships.Relationship;
import org.rhq.core.domain.resource.relationship.ResourceRelDefinition;

public class ResourceRelationshipMetadataParserTest {
    private static final Log LOG = LogFactory.getLog(ResourceRelationshipMetadataParserTest.class);
    private static final String DESCRIPTOR_FILENAME_TEST1 = "test-resource-relationships.xml";
    private PluginDescriptor pluginDescriptor;

    @BeforeSuite
    public void loadPluginDescriptor() throws Exception {
        try {
            URL descriptorUrl = this.getClass().getClassLoader().getResource(DESCRIPTOR_FILENAME_TEST1);
            LOG.info("Loading plugin descriptor at: " + descriptorUrl);

            JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);
            URL pluginSchemaURL = getClass().getClassLoader().getResource("rhq-plugin.xsd");
            Schema pluginSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
                pluginSchemaURL);

            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            ValidationEventCollector vec = new ValidationEventCollector();
            unmarshaller.setEventHandler(vec);
            unmarshaller.setSchema(pluginSchema);
            pluginDescriptor = (PluginDescriptor) unmarshaller.unmarshal(descriptorUrl.openStream());
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Test
    public void testDefinitionParsing1() {
        for (Relationship currR : pluginDescriptor.getRelationshipDescriptor().getRelationships()) {
            testRelationship(currR);
        }
    }

    public void testRelationship(Relationship r) {
        ResourceRelDefinition rrd = new ResourceRelDefinition();

        Assert.assertNotNull(r.getName());
        Assert.assertTrue(r.getName().length() > 0);
        Assert.assertNotNull(r.getCardinality());
        Assert.assertNotNull(rrd.getCardinality(r.getCardinality()));
        Assert.assertNotNull(rrd.getRelationshipType(r.getType()));

        System.out.println("!!Testing relationship name: " + r.getName() + ", cardinality: '" + r.getCardinality()
            + "', type '" + r.getType() + "', userEditable? '" + r.isUserEditable());

        System.out.println("\tSource Rel: type: " + r.getSource().getType() + ", plugin: " + r.getSource().getPlugin());
        Assert.assertNotNull(r.getSource());

        Assert.assertNotNull(r.getSource().getType());
        //Plugin is optional Assert.assertNotNull(r.getSource().getPlugin());

        System.out.println("\tSource Rel: type: " + r.getTarget().getType());
        Assert.assertNotNull(r.getTarget());
        Assert.assertNotNull(r.getTarget().getType());
    }
}
