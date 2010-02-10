/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
* All rights reserved.
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License, version 2, as
* published by the Free Software Foundation, and/or the GNU Lesser
* General Public License, version 2.1, also as published by the Free
* Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License and the GNU Lesser General Public License
* for more details.
*
* You should have received a copy of the GNU General Public License
* and the GNU Lesser General Public License along with this program;
* if not, write to the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/
package org.rhq.enterprise.server.content.test;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Advisory;
import org.rhq.core.domain.content.AdvisoryPackage;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.content.metadata.ContentSourceMetadataManagerLocal;
import org.rhq.enterprise.server.plugin.pc.content.TestContentServerPluginService;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jason Dobies
 */
public class ContentSourceRepoFlowTest extends AbstractEJB3Test {

    private static final boolean ENABLED = true;

    private RepoManagerLocal repoManager;
    private ContentSourceManagerLocal contentSourceManager;
    private ContentSourceMetadataManagerLocal contentSourceMetadataManager;
    private SubjectManagerLocal subjectManager;

    // The following will be cleaned up at the end of the test run

    private ResourceType resourceType;
    private PackageType packageType;
    private Package generalPackage;
    private PackageVersion packageVersion;
    private Advisory advisory;
    private ContentSource contentSource;
    private Repo repo;


    @BeforeMethod
    public void setup() throws Exception {
        repoManager = LookupUtil.getRepoManagerLocal();
        contentSourceManager = LookupUtil.getContentSourceManager();
        contentSourceMetadataManager = LookupUtil.getContentSourceMetadataManager();
        subjectManager = LookupUtil.getSubjectManager();

        prepareScheduler();
        new TestContentServerPluginService(this);

        initDatabase();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        cleanDatabase();

        unprepareServerPluginService();
        unprepareScheduler();
    }

    /**
     * Tests BZ 556848, running the following workflow:
     *
     * 1.  Create a content provider
     * 2.  Import a repo from it, where the repo has at least one advisory
     * 3.  Delete the repo
     * 4.  Delete the content provider
     *
     * The bug exposes a database relationship issue with package version and advisory.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLED)
    public void deleteSourceWithFullRepo() throws Exception {
        // Setup
        ContentSourceType csType = new ContentSourceType("deleteSourceWithFullRepo.cstype");
        Set<ContentSourceType> types = new HashSet<ContentSourceType>(1);
        types.add(csType);
        contentSourceMetadataManager.registerTypes(types);

        Subject overlord = subjectManager.getOverlord();

        // Test

        //  ( 1 )
        contentSource = new ContentSource("deleteSourceWithFullRepo.source", csType);
        contentSource = contentSourceManager.simpleCreateContentSource(overlord, contentSource);

        //  ( 2 )
        repo = new Repo("deleteSourceWithFullRepo.repo");
        repo.addContentSource(contentSource);
        repo = repoManager.createCandidateRepo(overlord, repo);

        //  ( 3 )
        repoManager.deleteRepo(overlord, repo.getId());

        //  ( 4 )
        contentSourceManager.deleteContentSource(overlord, contentSource.getId());

        // Verify

        // Part of the verification is that these calls did not blow up with constraint violations

        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        assert em.find(Advisory.class, advisory.getId()) == null;
        assert em.find(PackageVersion.class, packageVersion.getId()) == null;
        assert em.find(Repo.class, repo.getId()) == null;
        assert em.find(ContentSource.class, contentSource.getId()) == null;

        getTransactionManager().rollback();

        // Cleanup - Handled in tearDown
    }

    private void initDatabase() throws Exception {

        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        try {
            resourceType = new ResourceType("platform-" + System.currentTimeMillis(),
                "TestPlugin", ResourceCategory.PLATFORM, null);
            em.persist(resourceType);

            packageType = new PackageType("pkgType", resourceType);
            em.persist(packageType);

            generalPackage = new Package("pkg1", packageType);
            em.persist(generalPackage);

            Architecture architecture = em.find(Architecture.class, 1);
            packageVersion = new PackageVersion(generalPackage, "1.0", architecture);
            em.persist(packageVersion);

            advisory = new Advisory("adv1", "bugfix", "synop");
            Set<AdvisoryPackage> advisoryPackages = new HashSet<AdvisoryPackage>(1);
            advisoryPackages.add(new AdvisoryPackage(advisory, packageVersion));
            advisory.setAdvisorypkgs(advisoryPackages);
            em.persist(advisory);

            getTransactionManager().commit();
        }
        catch (Exception e) {
            getTransactionManager().rollback();
            throw e;
        }
        finally {
            em.close();
        }
    }

    private void cleanDatabase() throws Exception {

        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        try {

            // Most of these should be deleted in the test itself, but in case it fails we should
            // try to clean up.

            advisory = em.find(Advisory.class, advisory.getId());
            if (advisory != null) {
                em.remove(advisory);
            }

            packageVersion = em.find(PackageVersion.class, packageVersion.getId());
            if (packageVersion != null) {
                em.remove(packageVersion);
            }

            generalPackage = em.find(Package.class, generalPackage.getId());
            if (generalPackage != null) {
                em.remove(generalPackage);
            }

            packageType = em.find(PackageType.class,  packageType.getId());
            if (packageType != null) {
                em.remove(packageType);
            }

            resourceType = em.find(ResourceType.class, resourceType.getId());
            if (resourceType != null) {
                em.remove(resourceType);
            }

            repo = em.find(Repo.class, repo.getId());
            if (repo != null) {
                em.remove(repo);
            }

            contentSource = em.find(ContentSource.class, contentSource.getId());
            if (contentSource != null) {
                em.remove(contentSource);
            }

            getTransactionManager().commit();

            contentSourceMetadataManager.registerTypes(new HashSet<ContentSourceType>());
        }
        catch (Exception e) {
            getTransactionManager().rollback();
            throw e;
        }
        finally {
            em.close();
        }

    }

}
