/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.enterprise.server.drift;

import static java.util.Arrays.asList;
import static org.rhq.core.domain.common.EntityContext.forResource;
import static org.rhq.core.domain.drift.DriftCategory.FILE_ADDED;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.DRIFT;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext.fileSystem;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode.normal;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode.plannedChanges;
import static org.rhq.core.domain.drift.DriftDefinitionComparator.CompareMode.BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS;
import static org.rhq.enterprise.server.safeinvoker.HibernateDetachUtility.SerializationType.SERIALIZATION;
import static org.rhq.enterprise.server.util.LookupUtil.getDriftManager;
import static org.rhq.enterprise.server.util.LookupUtil.getDriftTemplateManager;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.EJBException;
import javax.persistence.EntityManager;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.DriftDefinitionCriteria;
import org.rhq.core.domain.criteria.DriftDefinitionTemplateCriteria;
import org.rhq.core.domain.criteria.GenericDriftChangeSetCriteria;
import org.rhq.core.domain.criteria.JPADriftChangeSetCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionComparator;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.drift.Filter;
import org.rhq.core.domain.drift.JPADrift;
import org.rhq.core.domain.drift.JPADriftChangeSet;
import org.rhq.core.domain.drift.JPADriftFile;
import org.rhq.core.domain.drift.JPADriftSet;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.safeinvoker.HibernateDetachUtility;
import org.rhq.test.TransactionCallback;

public class DriftTemplateManagerBeanTest extends DriftServerTest {

    private DriftTemplateManagerLocal templateMgr;

    private DriftManagerLocal driftMgr;

    private List<Resource> resources = new LinkedList<Resource>();

    private JPADrift drift1;
    private JPADrift drift2;
    private JPADriftFile driftFile1;
    private JPADriftFile driftFile2;

    @BeforeClass
    public void initClass() {
        templateMgr = getDriftTemplateManager();
        driftMgr = getDriftManager();

    }

    @Override
    protected void initDB(EntityManager em) {
        agentServiceContainer.driftService = new TestDefService() {
            @Override
            public void unscheduleDriftDetection(int resourceId, DriftDefinition driftDef) {
                detach(driftDef);
            }

            @Override
            public void updateDriftDetection(int resourceId, DriftDefinition driftDef) {
                detach(driftDef);
            }

            @Override
            public void updateDriftDetection(int resourceId, DriftDefinition driftDef, DriftSnapshot driftSnapshot) {
                detach(driftDef);
                detach(driftSnapshot);
            }

            private void detach(Object object) {
                try {
                    HibernateDetachUtility.nullOutUninitializedFields(object, SERIALIZATION);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public void createNewTemplate() {
        final DriftDefinition definition = new DriftDefinition(new Configuration());
        definition.setName("test::createNewTemplate");
        definition.setEnabled(true);
        definition.setDriftHandlingMode(normal);
        definition.setInterval(2400L);
        definition.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));

        final DriftDefinitionTemplate newTemplate = templateMgr.createTemplate(getOverlord(), resourceType.getId(),
            true, definition);

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();

                ResourceType updatedType = em.find(ResourceType.class, resourceType.getId());

                assertEquals("Failed to add new drift definition to resource type", 1, updatedType
                    .getDriftDefinitionTemplates().size());

                DriftDefinitionTemplate expectedTemplate = new DriftDefinitionTemplate();
                expectedTemplate.setTemplateDefinition(definition);
                expectedTemplate.setUserDefined(true);

                assertDriftTemplateEquals("Failed to save template", expectedTemplate, newTemplate);
                assertTrue("The template should have its id set", newTemplate.getId() > 0);
            }
        });
    }

    public void createTemplateForNegativeUpdateTests() {
        DriftDefinition definition = new DriftDefinition(new Configuration());
        definition.setName("test::createTemplateForNegativeUpdateTests");
        definition.setEnabled(true);
        definition.setDriftHandlingMode(normal);
        definition.setInterval(2400L);
        definition.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));

        templateMgr.createTemplate(getOverlord(), resourceType.getId(), true, definition);

        assertNotNull("Failed to load template", loadTemplate(definition.getName()));
    }

    @Test(dependsOnMethods = "createTemplateForNegativeUpdateTests",
        expectedExceptions = EJBException.class,
        expectedExceptionsMessageRegExp = ".*base directory.*cannot be modified")
    @InitDB(false)
    public void doNotAllowBaseDirToBeUpdated() {
        DriftDefinitionTemplate template = loadTemplate("test::createTemplateForNegativeUpdateTests");
        DriftDefinition definition = template.getTemplateDefinition();
        definition.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/TEST"));

        templateMgr.updateTemplate(getOverlord(), template);
    }

    @Test(dependsOnMethods = "createTemplateForNegativeUpdateTests",
        expectedExceptions = EJBException.class,
        expectedExceptionsMessageRegExp = ".*filters.*cannot be modified")
    @InitDB(false)
    public void doNotAllowFiltersToBeUpdated() {
        DriftDefinitionTemplate template = loadTemplate("test::createTemplateForNegativeUpdateTests");
        DriftDefinition definition = template.getTemplateDefinition();
        definition.addExclude(new Filter("/foo/bar/TEST/conf", "*.xml"));

        templateMgr.updateTemplate(getOverlord(), template);
    }

    @Test(dependsOnMethods = "createTemplateForNegativeUpdateTests",
        expectedExceptions = EJBException.class,
        expectedExceptionsMessageRegExp = ".*name.*cannot be modified")
    @InitDB(false)
    public void doNotAllowTemplateNameToBeUpdated() {
        DriftDefinitionTemplate template = loadTemplate("test::createTemplateForNegativeUpdateTests");
        template.setName("A new name");

        templateMgr.updateTemplate(getOverlord(), template);
    }

    public void createAndUpdateTemplate() {
        // create the template
        DriftDefinition definition = new DriftDefinition(new Configuration());
        definition.setName("test::updateTemplate");
        definition.setDescription("update template test");
        definition.setEnabled(true);
        definition.setDriftHandlingMode(normal);
        definition.setInterval(2400L);
        definition.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));

        DriftDefinitionTemplate template = templateMgr.createTemplate(getOverlord(), resourceType.getId(), true,
            definition);

        // next create some definitions from the template
        final DriftDefinition attachedDef1 = createDefinition(template, "attachedDef1", true);
        final DriftDefinition attachedDef2 = createDefinition(template, "attachedDef2", true);
        final DriftDefinition detachedDef1 = createDefinition(template, "detachedDef1", false);
        final DriftDefinition detachedDef2 = createDefinition(template, "detachedDef2", false);

        driftMgr.updateDriftDefinition(getOverlord(), forResource(resource.getId()), attachedDef1);
        driftMgr.updateDriftDefinition(getOverlord(), forResource(resource.getId()), attachedDef2);
        driftMgr.updateDriftDefinition(getOverlord(), forResource(resource.getId()), detachedDef1);
        driftMgr.updateDriftDefinition(getOverlord(), forResource(resource.getId()), detachedDef2);

        // update the template
        final DriftDefinition newTemplateDef = template.getTemplateDefinition();
        newTemplateDef.setInterval(4800L);
        newTemplateDef.setDriftHandlingMode(plannedChanges);
        newTemplateDef.setEnabled(false);

        templateMgr.updateTemplate(getOverlord(), template);

        // verify that the tempalte has been updated
        final DriftDefinitionTemplate updatedTemplate = loadTemplate(template.getName());
        assertPropertiesMatch("Failed to update template", template, updatedTemplate, "resourceType",
            "driftDefinitions", "templateDefinition");

        // verify that attached definitions are updated.
        for (DriftDefinition def : asList(attachedDef1, attachedDef2)) {
            DriftDefinition updatedDef = loadDefinition(def.getId());
            String msg = "Failed to propagate update to attached definition " + toString(updatedDef) + " - ";
            DriftDefinition updatedTemplateDef = updatedTemplate.getTemplateDefinition();

            assertEquals(msg + "enabled property not updated", updatedTemplateDef.isEnabled(),
                updatedDef.isEnabled());
            assertEquals(msg + "driftHandlingMode property not updated",
                updatedTemplateDef.getDriftHandlingMode(), updatedDef.getDriftHandlingMode());
            assertEquals(msg + "interval property not updated", updatedTemplateDef.getInterval(),
                updatedDef.getInterval());
        }

        // verify that the detached definitions have not been updated.
        for (DriftDefinition def : asList(detachedDef1, detachedDef2)) {
            DriftDefinition defAfterUpdate = loadDefinition(def.getId());
            String msg = "Detached definition " + toString(def) + " should not get updated - ";

            assertEquals(msg + "enabled property was modified", def.isEnabled(), defAfterUpdate.isEnabled());
            assertEquals(msg + "driftHandlingMode property was modified", def.getDriftHandlingMode(),
                defAfterUpdate.getDriftHandlingMode());
            assertEquals(msg + "interval property was modified", def.getInterval(), defAfterUpdate.getInterval());
        }
    }


    @SuppressWarnings("unchecked")
    public void pinTemplate() throws Exception {
        // First create the template
        final DriftDefinition templateDef = new DriftDefinition(new Configuration());
        templateDef.setName("test::pinTemplate");
        templateDef.setEnabled(true);
        templateDef.setDriftHandlingMode(normal);
        templateDef.setInterval(2400L);
        templateDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));

        final DriftDefinitionTemplate template = templateMgr.createTemplate(getOverlord(),
            resourceType.getId(), true, templateDef);

        // next create some resource level definitions
        final DriftDefinition attachedDef1 = createDefinition(template, "attachedDef1", true);
        final DriftDefinition attachedDef2 = createDefinition(template, "attachedDef2", true);
        final DriftDefinition detachedDef1 = createDefinition(template, "detachedDef1", false);
        final DriftDefinition detachedDef2 = createDefinition(template, "detachedDef2", false);

        // create initial change set from which the snapshot will be generated
        driftFile1 = new JPADriftFile("a1b2c3");
        drift1 = new JPADrift(null, "drift.1", FILE_ADDED, null, driftFile1);

        JPADriftSet driftSet = new JPADriftSet();
        driftSet.addDrift(drift1);

        final JPADriftChangeSet changeSet0 = new JPADriftChangeSet(resource, 0, COVERAGE, attachedDef1);
        changeSet0.setInitialDriftSet(driftSet);

        // create change set v1
        driftFile2 = new JPADriftFile("1a2b3c");
        final JPADriftChangeSet changeSet1 = new JPADriftChangeSet(resource, 1, DRIFT, attachedDef1);
        drift2 = new JPADrift(changeSet1, "drift.2", FILE_ADDED, null, driftFile2);

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                em.persist(attachedDef1);
                em.persist(driftFile1);
                em.persist(driftFile2);
                em.persist(drift1);
                em.persist(drift2);
                em.persist(changeSet0);
                em.persist(changeSet1);

                em.persist(attachedDef2);
                em.persist(detachedDef1);
                em.persist(detachedDef2);
            }
        });

        // now we pin the snapshot to the template
        templateMgr.pinTemplate(getOverlord(), template.getId(), attachedDef1.getId(), 1);

        // verify that the template is now pinned
        DriftDefinitionTemplate updatedTemplate = loadTemplate(template.getName());
        assertTrue("Template should be marked pinned", updatedTemplate.isPinned());
    }

    @Test(dependsOnMethods = "pinTemplate")
    @InitDB(false)
    public void persistChangeSetWhenTemplateGetsPinned() throws Exception {
        DriftDefinitionTemplate template = loadTemplate("test::pinTemplate");

        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterId(template.getChangeSetId());

        PageList<? extends DriftChangeSet<?>> changeSets = driftMgr.findDriftChangeSetsByCriteria(getOverlord(),
            criteria);

        assertEquals("Expected to find change set for pinned template", 1, changeSets.size());

        JPADriftChangeSet expectedChangeSet = new JPADriftChangeSet(resource, 1, COVERAGE, null);
        List<? extends Drift> expectedDrifts = asList(
            new JPADrift(expectedChangeSet, "drift.1", FILE_ADDED, null, driftFile1),
            new JPADrift(expectedChangeSet, drift2.getPath(), FILE_ADDED, null, driftFile2));

        DriftChangeSet<?> actualChangeSet = changeSets.get(0);
        List<? extends Drift> actualDrifts = new ArrayList(actualChangeSet.getDrifts());

        assertCollectionMatchesNoOrder("Expected to find drifts from change sets 1 and 2 in the template change set",
            (List<Drift>)expectedDrifts, (List<Drift>)actualDrifts, "id", "ctime", "changeSet", "newDriftFile");

        // we need to compare the newDriftFile properties separately because
        // assertCollectionMatchesNoOrder compares properties via equals() and JPADriftFile
        // does not implement equals.
        assertPropertiesMatch(drift1.getNewDriftFile(), findDriftByPath(actualDrifts, "drift.1").getNewDriftFile(),
            "The newDriftFile property was not set correctly for " + drift1);
        assertPropertiesMatch(drift2.getNewDriftFile(), findDriftByPath(actualDrifts, "drift.2").getNewDriftFile(),
            "The newDriftFile property was not set correctly for " + drift1);
    }

    @Test(dependsOnMethods = "pinTemplate")
    @InitDB(false)
    public void updateAttachedDefinitionsWhenTemplateGetsPinned() throws Exception {
        DriftDefinitionTemplate template = loadTemplate("test::pinTemplate");

        // get the attached definitions
        List<DriftDefinition> attachedDefs = new LinkedList<DriftDefinition>();
        for (DriftDefinition d : template.getDriftDefinitions()) {
            if (d.isAttached() && (d.getName().equals("attachedDef1") || d.getName().equals("attachedDef2"))) {
                attachedDefs.add(d);
            }
        }
        assertEquals("Failed to get attached definitions for " + toString(template), 2, attachedDefs.size());
        assertDefinitionIsPinned(attachedDefs.get(0));
        assertDefinitionIsPinned(attachedDefs.get(1));
    }

    @Test(dependsOnMethods = "pinTemplate")
    @InitDB(false)
    public void doNotUpdateDetachedDefinitionsWhenTemplateGetsPinned() throws Exception {
        DriftDefinitionTemplate template = loadTemplate("test::pinTemplate");

        // get the detached definitions
        List<DriftDefinition> detachedDefs = new LinkedList<DriftDefinition>();
        for (DriftDefinition d : template.getDriftDefinitions()) {
            if (!d.isAttached() && (d.getName().equals("detachedDef1") || d.getName().equals("detachedDef2"))) {
                detachedDefs.add(d);
            }
        }
        assertEquals("Failed to get detached definitions for " + toString(template), 2, detachedDefs.size());
        assertDefinitionIsNotPinned(detachedDefs.get(0));
        assertDefinitionIsNotPinned(detachedDefs.get(1));
    }

    public void deleteTemplate() throws Exception {
        // first create the template
        final DriftDefinition templateDef = new DriftDefinition(new Configuration());
        templateDef.setName("test::pinTemplate");
        templateDef.setEnabled(true);
        templateDef.setDriftHandlingMode(normal);
        templateDef.setInterval(2400L);
        templateDef.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));

        final DriftDefinitionTemplate template = templateMgr.createTemplate(getOverlord(),
            resourceType.getId(), true, templateDef);

        // next create some resource level definitions
        final DriftDefinition attachedDef1 = createDefinition(template, "attachedDef1", true);
        final DriftDefinition attachedDef2 = createDefinition(template, "attachedDef2", true);
        final DriftDefinition detachedDef1 = createDefinition(template, "detachedDef1", false);
        final DriftDefinition detachedDef2 = createDefinition(template, "detachedDef2", false);

        // create some change sets
        driftFile1 = new JPADriftFile("a1b2c3");
        drift1 = new JPADrift(null, "drift.1", FILE_ADDED, null, driftFile1);

        JPADriftSet driftSet0 = new JPADriftSet();
        driftSet0.addDrift(drift1);

        final JPADriftChangeSet changeSet0 = new JPADriftChangeSet(resource, 0, COVERAGE, attachedDef1);
        changeSet0.setInitialDriftSet(driftSet0);


        driftFile2 = new JPADriftFile("1a2b3c");
        drift2 = new JPADrift(null, "drift.2", FILE_ADDED, null, driftFile2);

        JPADriftSet driftSet1 = new JPADriftSet();
        driftSet1.addDrift(drift2);

        final JPADriftChangeSet changeSet1 = new JPADriftChangeSet(resource, 0, DRIFT, detachedDef1);
        changeSet1.setInitialDriftSet(driftSet1);

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                em.persist(attachedDef1);
                em.persist(attachedDef2);
                em.persist(detachedDef1);
                em.persist(detachedDef2);
                em.persist(driftFile1);
                em.persist(driftFile2);
                em.persist(drift1);
                em.persist(drift2);
                em.persist(changeSet0);
                em.persist(changeSet1);
            }
        });

        // delete the template
        templateMgr.deleteTemplate(getOverlord(), template.getId());

        // verify that attached definitions along with their change sets have
        // been deleted
        assertNull("Change sets belonging to attached definitions should be deleted", loadChangeSet(changeSet0.getId()));
        assertNull("Attached definition " + toString(attachedDef1) + " should be deleted",
            loadDefinition(attachedDef1.getId()));
        assertNull("Attached definition " + toString(attachedDef2) + " should be deleted",
            loadDefinition(attachedDef2.getId()));

        // verify that detached definitions along with their change sets have not been deleted
        assertNotNull("Change sets belonging to detached definitions should not be deleted",
            loadChangeSet(changeSet1.getId()));
        assertDetachedDefinitionNotDeleted(detachedDef1.getId());
        assertDetachedDefinitionNotDeleted(detachedDef2.getId());

        // verify that the template itself has been deleted
        assertNull("The template " + toString(template) + " should have been deleted",
            loadTemplate(template.getName(), false));
    }

    private void assertDefinitionIsPinned(DriftDefinition definition) throws Exception {
        // verify that the definition is marked as pinned
        assertTrue("Expected " + toString(definition) + " to be pinned", definition.isPinned());

        // verify that the initial change set is generated for the definition
        JPADriftChangeSetCriteria criteria = new JPADriftChangeSetCriteria();
        criteria.addFilterDriftDefinitionId(definition.getId());
        criteria.addFilterCategory(COVERAGE);
        criteria.fetchDrifts(true);

        PageList<? extends DriftChangeSet<?>> changeSets = driftMgr.findDriftChangeSetsByCriteria(getOverlord(),
            criteria);
        assertEquals("Expected to find one change set", 1, changeSets.size());

        JPADriftChangeSet expectedChangeSet = new JPADriftChangeSet(resource, 1, COVERAGE, null);
        List<? extends Drift> expectedDrifts = asList(
            new JPADrift(expectedChangeSet, drift1.getPath(), FILE_ADDED, null, driftFile1),
            new JPADrift(expectedChangeSet, drift2.getPath(), FILE_ADDED, null, driftFile2));

        DriftChangeSet<?> actualChangeSet = changeSets.get(0);
        List<? extends Drift> actualDrifts = new ArrayList(actualChangeSet.getDrifts());

        assertCollectionMatchesNoOrder("Expected to find drifts from change sets 1 and 2 in the template change set",
            (List<Drift>)expectedDrifts, (List<Drift>)actualDrifts, "id", "ctime", "changeSet", "newDriftFile");

        // Finally make sure that there are no other change sets
        criteria = new JPADriftChangeSetCriteria();
        criteria.addFilterStartVersion(1);
        criteria.addFilterDriftDefinitionId(definition.getId());

        assertEquals("There should not be any drift change sets", 0,
            driftMgr.findDriftChangeSetsByCriteria(getOverlord(), criteria).size());
    }

    private void assertDefinitionIsNotPinned(DriftDefinition definition) throws Exception {
        // verify that the definition is not pinned
        assertFalse("Expected " + toString(definition) + " to be unpinned", definition.isPinned());

        // Note that this method assumes that the definition has no change sets
        // associated with it and therefore checks that there are no change sets.
        JPADriftChangeSetCriteria criteria = new JPADriftChangeSetCriteria();
        criteria.addFilterDriftDefinitionId(definition.getId());

        PageList<? extends DriftChangeSet<?>> changeSets = driftMgr.findDriftChangeSetsByCriteria(getOverlord(),
            criteria);
        assertEquals("Did not expect to find any change sets for " + toString(definition) + ". Note that this " +
            "assertion method assumes that the definition you are testing is not supposed to have any change sets.",
            0, changeSets.size());
    }

    private void assertDriftTemplateEquals(String msg, DriftDefinitionTemplate expected, DriftDefinitionTemplate actual) {
        assertPropertiesMatch(msg + ": basic drift definition template properties do not match", expected, actual,
            "id", "resourceType", "ctime", "templateDefinition");
        assertDriftDefEquals(msg + ": template definitions do not match", expected.getTemplateDefinition(), actual
            .getTemplateDefinition());
    }

    private void assertDriftDefEquals(String msg, DriftDefinition expected, DriftDefinition actual) {
        DriftDefinitionComparator comparator = new DriftDefinitionComparator(
            BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS);
        assertEquals(msg, 0, comparator.compare(expected, actual));
    }

    private void assertDetachedDefinitionNotDeleted(int definitionId) {
        DriftDefinition definition = loadDefinition(definitionId);
        assertNotNull("Detached definition " + toString(definition) + " should not be deleted", definition);
        assertNull("The detached definition's template reference should be set to null when the template is deleted",
            definition.getTemplate());
    }

    private DriftDefinition createDefinition(DriftDefinitionTemplate template, String defName, boolean isAttached) {
        DriftDefinition def = template.createDefinition();
        def.setName(defName);
        def.setAttached(isAttached);
        def.setTemplate(template);
        def.setResource(resource);
        return def;
    }

    private DriftDefinitionTemplate loadTemplate(String name) {
        return loadTemplate(name, true);
    }

    private DriftDefinitionTemplate loadTemplate(String name, boolean verifyResultsUnique) {
        DriftDefinitionTemplateCriteria criteria = new DriftDefinitionTemplateCriteria();
        criteria.addFilterResourceTypeId(resourceType.getId());
        criteria.addFilterName(name);
        criteria.fetchDriftDefinitions(true);

        PageList<DriftDefinitionTemplate> templates = templateMgr.findTemplatesByCriteria(getOverlord(), criteria);
        if (verifyResultsUnique) {
            assertEquals("Expected to find one template", 1, templates.size());
        }

        if (templates.isEmpty()) {
            return null;
        }
        return templates.get(0);
    }

    private DriftDefinition loadDefinition(int definitionId) {
        DriftDefinitionCriteria criteria = new DriftDefinitionCriteria();
        criteria.addFilterId(definitionId);
        criteria.fetchConfiguration(true);
        criteria.fetchTemplate(true);
        PageList<DriftDefinition> definitions = driftMgr.findDriftDefinitionsByCriteria(getOverlord(), criteria);

        if (definitions.isEmpty()) {
            return null;
        }
        return definitions.get(0);
    }

    private DriftChangeSet<?> loadChangeSet(String id) throws Exception {
        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterId(id);
        PageList<? extends DriftChangeSet<?>> changeSets = driftMgr.findDriftChangeSetsByCriteria(getOverlord(),
            criteria);

        if (changeSets.isEmpty()) {
            return null;
        }
        return changeSets.get(0);
    }

}
