/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.content;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.annotation.IgnoreDependency;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceSyncResults;
import org.rhq.core.domain.content.ContentSourceSyncStatus;
import org.rhq.core.domain.content.Distribution;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.PackageVersionContentSource;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoContentSource;
import org.rhq.core.domain.content.RepoDistribution;
import org.rhq.core.domain.content.RepoGroup;
import org.rhq.core.domain.content.RepoGroupType;
import org.rhq.core.domain.content.RepoPackageVersion;
import org.rhq.core.domain.content.RepoRelationship;
import org.rhq.core.domain.content.RepoRelationshipType;
import org.rhq.core.domain.content.RepoRepoRelationship;
import org.rhq.core.domain.content.ResourceRepo;
import org.rhq.core.domain.content.composite.RepoComposite;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.plugin.pc.content.ContentServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.content.RepoDetails;
import org.rhq.enterprise.server.plugin.pc.content.RepoGroupDetails;
import org.rhq.enterprise.server.plugin.pc.content.RepoImportReport;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

@Stateless
public class RepoManagerBean implements RepoManagerLocal, RepoManagerRemote {

    /**
     * Refers to the default repo relationship created at DB setup time to represent a parent/child repo
     * relationship.
     * <p/>
     * This probably isn't the best place to store this, but for now this is the primary usage of this
     * relationship type.
     */
    private static final String PARENT_RELATIONSHIP_NAME = "parent";

    private final Log log = LogFactory.getLog(RepoManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authzManager;

    @IgnoreDependency
    @EJB
    private ContentSourceManagerLocal contentSourceManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void deleteRepo(Subject subject, int repoId) {
        log.debug("User [" + subject + "] is deleting repo [" + repoId + "]");

        // bulk delete m-2-m mappings to the doomed repo
        // get ready for bulk delete by clearing entity manager
        entityManager.flush();
        entityManager.clear();

        entityManager.createNamedQuery(ResourceRepo.DELETE_BY_REPO_ID).setParameter("repoId", repoId).executeUpdate();

        entityManager.createNamedQuery(RepoContentSource.DELETE_BY_REPO_ID).setParameter("repoId", repoId)
            .executeUpdate();

        entityManager.createNamedQuery(RepoPackageVersion.DELETE_BY_REPO_ID).setParameter("repoId", repoId)
            .executeUpdate();

        Repo repo = entityManager.find(Repo.class, repoId);
        if (repo != null) {
            entityManager.remove(repo);
            log.debug("User [" + subject + "] deleted repo [" + repo + "]");
        } else {
            log.debug("Repo ID [" + repoId + "] doesn't exist - nothing to delete");
        }

        // remove any unused, orphaned package versions
        contentSourceManager.purgeOrphanedPackageVersions(subject);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void deleteRepoGroup(Subject subject, int repoGroupId) {
        RepoGroup deleteMe = getRepoGroup(subject, repoGroupId);
        entityManager.remove(deleteMe);
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<Repo> findRepos(Subject subject, PageControl pc) {
        pc.initDefaultOrderingField("c.name");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, Repo.QUERY_FIND_ALL_IMPORTED_REPOS, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, Repo.QUERY_FIND_ALL_IMPORTED_REPOS);

        List<Repo> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<Repo>(results, (int) count, pc);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Repo getRepo(Subject subject, int repoId) {
        Repo repo = entityManager.find(Repo.class, repoId);

        if ((repo != null) && (repo.getRepoContentSources() != null)) {
            // load content sources separately. we can't do this all at once via fetch join because
            // on Oracle we use a LOB column on a content source field and you can't DISTINCT on LOBs
            repo.getRepoContentSources().size();
        }

        return repo;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public RepoGroup getRepoGroup(Subject subject, int repoGroupId) {
        RepoGroup repoGroup = entityManager.find(RepoGroup.class, repoGroupId);
        return repoGroup;
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<ContentSource> findAssociatedContentSources(Subject subject, int repoId, PageControl pc) {
        pc.initDefaultOrderingField("cs.id");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, ContentSource.QUERY_FIND_BY_REPO_ID, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, ContentSource.QUERY_FIND_BY_REPO_ID);

        query.setParameter("id", repoId);
        countQuery.setParameter("id", repoId);

        List<ContentSource> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<ContentSource>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<Resource> findSubscribedResources(Subject subject, int repoId, PageControl pc) {
        pc.initDefaultOrderingField("rc.resource.id");

        Query query = PersistenceUtility
            .createQueryWithOrderBy(entityManager, Repo.QUERY_FIND_SUBSCRIBER_RESOURCES, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, Repo.QUERY_FIND_SUBSCRIBER_RESOURCES);

        query.setParameter("id", repoId);
        countQuery.setParameter("id", repoId);

        List<Resource> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<Resource>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    // current resource subscriptions should be viewing, but perhaps available ones shouldn't
    public PageList<RepoComposite> findResourceSubscriptions(Subject subject, int resourceId, PageControl pc) {
        pc.initDefaultOrderingField("c.id");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            Repo.QUERY_FIND_REPO_COMPOSITES_BY_RESOURCE_ID, pc);
        Query countQuery = entityManager.createNamedQuery(Repo.QUERY_FIND_REPO_COMPOSITES_BY_RESOURCE_ID_COUNT);

        query.setParameter("resourceId", resourceId);
        countQuery.setParameter("resourceId", resourceId);

        List<RepoComposite> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<RepoComposite>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<RepoComposite> findAvailableResourceSubscriptions(Subject subject, int resourceId, PageControl pc) {
        pc.initDefaultOrderingField("c.id");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            Repo.QUERY_FIND_AVAILABLE_REPO_COMPOSITES_BY_RESOURCE_ID, pc);
        Query countQuery = entityManager
            .createNamedQuery(Repo.QUERY_FIND_AVAILABLE_REPO_COMPOSITES_BY_RESOURCE_ID_COUNT);

        query.setParameter("resourceId", resourceId);
        countQuery.setParameter("resourceId", resourceId);

        List<RepoComposite> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<RepoComposite>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    public List<RepoComposite> findResourceSubscriptions(int resourceId) {
        Query query = entityManager.createNamedQuery(Repo.QUERY_FIND_REPO_COMPOSITES_BY_RESOURCE_ID);

        query.setParameter("resourceId", resourceId);

        List<RepoComposite> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    public List<RepoComposite> findAvailableResourceSubscriptions(int resourceId) {
        Query query = entityManager.createNamedQuery(Repo.QUERY_FIND_AVAILABLE_REPO_COMPOSITES_BY_RESOURCE_ID);

        query.setParameter("resourceId", resourceId);

        List<RepoComposite> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<PackageVersion> findPackageVersionsInRepo(Subject subject, int repoId, PageControl pc) {
        pc.initDefaultOrderingField("pv.generalPackage.name, pv.version");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PackageVersion.QUERY_FIND_BY_REPO_ID_WITH_PACKAGE, pc);

        query.setParameter("repoId", repoId);

        List<PackageVersion> results = query.getResultList();
        long count = getPackageVersionCountFromRepo(subject, null, repoId);

        return new PageList<PackageVersion>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<PackageVersion> findPackageVersionsInRepo(Subject subject, int repoId, String filter, PageControl pc) {
        pc.initDefaultOrderingField("pv.generalPackage.name, pv.version");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PackageVersion.QUERY_FIND_BY_REPO_ID_WITH_PACKAGE_FILTERED, pc);

        query.setParameter("repoId", repoId);
        query.setParameter("filter", PersistenceUtility.formatSearchParameter(filter));

        List<PackageVersion> results = query.getResultList();
        long count = getPackageVersionCountFromRepo(subject, filter, repoId);

        return new PageList<PackageVersion>(results, (int) count, pc);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Repo updateRepo(Subject subject, Repo repo) throws RepoException {
        if (repo.getName() == null || repo.getName().trim().equals("")) {
            throw new RepoException("Repo name is required");
        }

        // should we check non-null repo relationships and warn that we aren't changing them?
        log.debug("User [" + subject + "] is updating repo [" + repo + "]");
        repo = entityManager.merge(repo);
        log.debug("User [" + subject + "] updated repo [" + repo + "]");

        return repo;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Repo createRepo(Subject subject, Repo repo) throws RepoException {
        validateRepo(repo);

        repo.setCandidate(false);

        log.debug("User [" + subject + "] is creating repo [" + repo + "]");
        entityManager.persist(repo);
        log.debug("User [" + subject + "] created repo [" + repo + "]");

        return repo; // now has the ID set
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Repo createCandidateRepo(Subject subject, Repo repo) throws RepoException {
        validateRepo(repo);

        repo.setCandidate(true);

        entityManager.persist(repo);

        return repo;
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void deleteCandidatesWithOnlyContentSource(Subject subject, int contentSourceId) {
        Query query = entityManager.createNamedQuery(Repo.QUERY_FIND_CANDIDATES_WITH_ONLY_CONTENT_SOURCE);

        query.setParameter("contentSourceId", contentSourceId);

        List<Repo> repoList = query.getResultList();

        for (Repo deleteMe : repoList) {
            deleteRepo(subject, deleteMe.getId());
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void processRepoImportReport(Subject subject, RepoImportReport report, int contentSourceId,
        StringBuilder result) {

        // Import groups first
        List<RepoGroupDetails> repoGroups = report.getRepoGroups();
        int repoGroupCounter = 0;

        for (RepoGroupDetails createMe : repoGroups) {
            String name = createMe.getName();

            RepoGroup existingGroup = getRepoGroupByName(name);
            if (existingGroup == null) {
                existingGroup = new RepoGroup(name);
                existingGroup.setDescription(createMe.getDescription());

                RepoGroupType groupType = getRepoGroupTypeByName(subject, createMe.getTypeName());
                existingGroup.setRepoGroupType(groupType);

                // Don't let the whole report blow up if one of these fails,
                // but be sure to mention it to the report
                try {
                    createRepoGroup(subject, existingGroup);
                    repoGroupCounter++;
                } catch (RepoException e) {

                    if (e.getType() == RepoException.RepoExceptionType.NAME_ALREADY_EXISTS) {
                        result.append("Skipping existing repo group [").append(name).append("]").append('\n');
                    } else {
                        log.error("Error adding repo group [" + name + "]", e);
                        result.append("Could not add repo group [").append(name).append(
                            "]. See log for more information.").append('\n');
                    }
                }
            }
        }
        result.append("Imported [").append(repoGroupCounter).append("] repo groups.").append('\n');

        // Hold on to all current candidate repos for the content provider. If any were not present in this
        // report, remove them from the system (the rationale being, the content provider no longer knows
        // about them and thus they cannot be imported).
        RepoCriteria candidateReposCriteria = new RepoCriteria();
        candidateReposCriteria.addFilterContentSourceIds(contentSourceId);
        candidateReposCriteria.addFilterCandidate(true);

        PageList<Repo> candidatesForThisProvider = findReposByCriteria(subject, candidateReposCriteria);

        // Once the groups are in the system, import any repos that were added
        List<RepoDetails> repos = report.getRepos();
        int repoCounter = 0;

        // First add repos that have no parent. We later add repos with a parent afterwards to prevent
        // issues where both the parent and child are specified in this report.
        for (RepoDetails createMe : repos) {

            if (createMe.getParentRepoName() == null) {
                try {
                    addCandidateRepo(contentSourceId, createMe);
                    removeRepoFromList(createMe.getName(), candidatesForThisProvider);
                    repoCounter++;
                } catch (Exception e) {

                    if (e instanceof RepoException
                        && ((RepoException) e).getType() == RepoException.RepoExceptionType.NAME_ALREADY_EXISTS) {
                        result.append("Skipping addition of existing repo [").append(createMe.getName()).append("]")
                            .append('\n');
                    } else {
                        log.error("Error processing repo [" + createMe + "]", e);
                        result.append("Could not add repo [").append(createMe.getName()).append(
                            "]. See log for more information.").append('\n');
                    }
                }
            }
        }

        // Take a second pass through the list checking for any repos that were created to be
        // a child of another repo.
        for (RepoDetails createMe : repos) {

            if (createMe.getParentRepoName() != null) {
                try {
                    addCandidateRepo(contentSourceId, createMe);
                    removeRepoFromList(createMe.getName(), candidatesForThisProvider);
                    repoCounter++;
                } catch (Exception e) {
                    log.error("Error processing repo [" + createMe + "]", e);
                    result.append("Could not add repo [").append(createMe.getName()).append(
                        "]. See log for more information.").append('\n');
                }
            }
        }

        result.append("Imported [").append(repoCounter).append("] repos.").append('\n');

        // Any repos that haven't been removed from candidatesForThisProvider were not returned in this
        // report, so remove them from the database.
        for (Repo deleteMe : candidatesForThisProvider) {
            deleteRepo(subject, deleteMe.getId());
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void importCandidateRepo(Subject subject, List<Integer> repoIds) throws RepoException {

        for (Integer repoId : repoIds) {
            Repo repo = entityManager.find(Repo.class, repoId);

            if (repo == null) {
                throw new RepoException("Unable to find candidate repo for import. ID: " + repoId);
            }

            if (!repo.isCandidate()) {
                throw new RepoException("Unable to import repo, repo is already imported. ID: " + repoId);
            }

            repo.setCandidate(false);
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public RepoGroup createRepoGroup(Subject subject, RepoGroup repoGroup) throws RepoException {
        validateRepoGroup(repoGroup);

        entityManager.persist(repoGroup);

        return repoGroup;
    }

    @SuppressWarnings("unchecked")
    public List<Repo> getRepoByName(String name) {
        Query query = entityManager.createNamedQuery(Repo.QUERY_FIND_BY_NAME);

        query.setParameter("name", name);
        List<Repo> results = query.getResultList();

        return results;
    }

    @SuppressWarnings("unchecked")
    public RepoGroup getRepoGroupByName(String name) {
        Query query = entityManager.createNamedQuery(RepoGroup.QUERY_FIND_BY_NAME);

        query.setParameter("name", name);
        List<RepoGroup> results = query.getResultList();

        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public RepoGroupType getRepoGroupTypeByName(Subject subject, String name) {
        Query query = entityManager.createNamedQuery(RepoGroupType.QUERY_FIND_BY_NAME);

        query.setParameter("name", name);
        List<RepoGroupType> results = query.getResultList();

        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void addContentSourcesToRepo(Subject subject, int repoId, int[] contentSourceIds) throws Exception {
        Repo repo = entityManager.find(Repo.class, repoId);
        if (repo == null) {
            throw new Exception("There is no repo with an ID [" + repoId + "]");
        }

        repo.setLastModifiedDate(System.currentTimeMillis());

        log.debug("User [" + subject + "] is adding content sources to repo [" + repo + "]");

        ContentServerPluginContainer pc = ContentManagerHelper.getPluginContainer();
        Query q = entityManager.createNamedQuery(PackageVersionContentSource.QUERY_FIND_BY_CONTENT_SOURCE_ID_NO_FETCH);

        for (int id : contentSourceIds) {
            ContentSource cs = entityManager.find(ContentSource.class, id);
            if (cs == null) {
                throw new Exception("There is no content source with id [" + id + "]");
            }

            RepoContentSource ccsmapping = repo.addContentSource(cs);
            entityManager.persist(ccsmapping);

            Set<PackageVersion> alreadyAssociatedPVs = new HashSet<PackageVersion>(repo.getPackageVersions());

            // automatically associate all of the content source's package versions with this repo
            // but, *skip* over the ones that are already linked to this repo from a previous association
            q.setParameter("id", cs.getId());
            List<PackageVersionContentSource> pvcss = q.getResultList();
            for (PackageVersionContentSource pvcs : pvcss) {
                PackageVersion pv = pvcs.getPackageVersionContentSourcePK().getPackageVersion();
                if (alreadyAssociatedPVs.contains(pv)) {
                    continue; // skip if already associated with this repo
                }
                RepoPackageVersion mapping = new RepoPackageVersion(repo, pv);
                entityManager.persist(mapping);
            }

            entityManager.flush();
            entityManager.clear();

            // ask to synchronize the content source immediately (is this the right thing to do?)
            pc.syncNow(cs);
        }

        return;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void addPackageVersionsToRepo(Subject subject, int repoId, int[] packageVersionIds) {
        Repo repo = entityManager.find(Repo.class, repoId);

        for (int packageVersionId : packageVersionIds) {
            PackageVersion packageVersion = entityManager.find(PackageVersion.class, packageVersionId);

            RepoPackageVersion mapping = new RepoPackageVersion(repo, packageVersion);
            entityManager.persist(mapping);
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void removeContentSourcesFromRepo(Subject subject, int repoId, int[] contentSourceIds) throws RepoException {
        Repo repo = getRepo(subject, repoId);

        log.debug("User [" + subject + "] is removing content sources from repo [" + repo + "]");

        Set<RepoContentSource> currentSet = repo.getRepoContentSources();

        if ((currentSet != null) && (currentSet.size() > 0)) {
            Set<RepoContentSource> toBeRemoved = new HashSet<RepoContentSource>();
            for (RepoContentSource current : currentSet) {
                for (int id : contentSourceIds) {
                    if (id == current.getRepoContentSourcePK().getContentSource().getId()) {
                        toBeRemoved.add(current);
                        break;
                    }
                }
            }

            for (RepoContentSource doomed : toBeRemoved) {
                entityManager.remove(doomed);
            }

            currentSet.removeAll(toBeRemoved);
        }

        // note that we specifically do not disassociate package versions from the repo, even if those
        // package versions come from the content source that is being removed
    }

    @SuppressWarnings("unchecked")
    public void subscribeResourceToRepos(Subject subject, int resourceId, int[] repoIds) {
        if ((repoIds == null) || (repoIds.length == 0)) {
            return; // nothing to do
        }

        // make sure the user has permissions to subscribe this resource
        if (!authzManager.hasResourcePermission(subject, Permission.MANAGE_CONTENT, resourceId)) {
            throw new PermissionException("[" + subject
                + "] does not have permission to subscribe this resource to repos");
        }

        // find the resource - abort if it does not exist
        Resource resource = entityManager.find(Resource.class, resourceId);
        if (resource == null) {
            throw new RuntimeException("There is no resource with the ID [" + resourceId + "]");
        }

        // find all the repos and subscribe the resource to each of them
        // note that if the length of the ID array doesn't match, then one of the repos doesn't exist
        // and we abort altogether - we do not subscribe to anything unless all repo IDs are valid
        Query q = entityManager.createNamedQuery(Repo.QUERY_FIND_BY_IDS);
        List<Integer> idList = new ArrayList<Integer>(repoIds.length);
        for (Integer id : repoIds) {
            idList.add(id);
        }

        q.setParameter("ids", idList);
        List<Repo> repos = q.getResultList();

        if (repos.size() != repoIds.length) {
            throw new RuntimeException("One or more of the repos do not exist [" + idList + "]->[" + repos + "]");
        }

        for (Repo repo : repos) {
            ResourceRepo mapping = repo.addResource(resource);
            entityManager.persist(mapping);
        }
    }

    @SuppressWarnings("unchecked")
    public void unsubscribeResourceFromRepos(Subject subject, int resourceId, int[] repoIds) {
        if ((repoIds == null) || (repoIds.length == 0)) {
            return; // nothing to do
        }

        // make sure the user has permissions to unsubscribe this resource
        if (!authzManager.hasResourcePermission(subject, Permission.MANAGE_CONTENT, resourceId)) {
            throw new PermissionException("[" + subject
                + "] does not have permission to unsubscribe this resource from repos");
        }

        // find the resource - abort if it does not exist
        Resource resource = entityManager.find(Resource.class, resourceId);
        if (resource == null) {
            throw new RuntimeException("There is no resource with the ID [" + resourceId + "]");
        }

        // find all the repos and unsubscribe the resource from each of them
        // note that if the length of the ID array doesn't match, then one of the repos doesn't exist
        // and we abort altogether - we do not unsubscribe from anything unless all repo IDs are valid
        Query q = entityManager.createNamedQuery(Repo.QUERY_FIND_BY_IDS);
        List<Integer> idList = new ArrayList<Integer>(repoIds.length);
        for (Integer id : repoIds) {
            idList.add(id);
        }

        q.setParameter("ids", idList);
        List<Repo> repos = q.getResultList();

        if (repos.size() != repoIds.length) {
            throw new RuntimeException("One or more of the repos do not exist [" + idList + "]->[" + repos + "]");
        }

        for (Repo repo : repos) {
            ResourceRepo mapping = repo.removeResource(resource);
            entityManager.remove(mapping);
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public long getPackageVersionCountFromRepo(Subject subject, String filter, int repoId) {
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            PackageVersion.QUERY_FIND_BY_REPO_ID_FILTERED);

        countQuery.setParameter("repoId", repoId);
        countQuery.setParameter("filter", (filter == null) ? null : ("%" + filter.toUpperCase() + "%"));

        return ((Long) countQuery.getSingleResult()).longValue();
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public long getPackageVersionCountFromRepo(Subject subject, int repoId) {
        return getPackageVersionCountFromRepo(subject, null, repoId);
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<Repo> findReposByCriteria(Subject subject, RepoCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);

        CriteriaQueryRunner<Repo> queryRunner = new CriteriaQueryRunner(criteria, generator, entityManager);
        return queryRunner.execute();
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<PackageVersion> findPackageVersionsInRepoByCriteria(Subject subject, PackageVersionCriteria criteria) {
        Integer repoId = criteria.getFilterRepoId();

        if ((null == repoId) || (repoId < 1)) {
            throw new IllegalArgumentException("Illegal filterResourceId: " + repoId);
        }

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);

        Query query = generator.getQuery(entityManager);
        Query countQuery = generator.getCountQuery(entityManager);

        long count = (Long) countQuery.getSingleResult();
        List<PackageVersion> packageVersions = query.getResultList();

        return new PageList<PackageVersion>(packageVersions, (int) count, criteria.getPageControl());
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void addRepoRelationship(Subject subject, int repoId, int relatedRepoId, String relationshipTypeName) {

        Repo repo = entityManager.find(Repo.class, repoId);
        Repo relatedRepo = entityManager.find(Repo.class, relatedRepoId);

        Query typeQuery = entityManager.createNamedQuery(RepoRelationshipType.QUERY_FIND_BY_NAME);
        typeQuery.setParameter("name", relationshipTypeName);
        RepoRelationshipType relationshipType = (RepoRelationshipType) typeQuery.getSingleResult();

        RepoRelationship repoRelationship = new RepoRelationship();
        repoRelationship.setRelatedRepo(relatedRepo);
        repoRelationship.setRepoRelationshipType(relationshipType);
        repoRelationship.addRepo(repo);

        entityManager.persist(repoRelationship);
        relatedRepo.addRepoRelationship(repoRelationship);

        RepoRepoRelationship repoRepoRelationship = new RepoRepoRelationship(repo, repoRelationship);

        entityManager.persist(repoRepoRelationship);
        repo.addRepoRelationship(repoRelationship);
    }

    private void validateRepo(Repo c) throws RepoException {
        if (c.getName() == null || c.getName().trim().equals("")) {
            throw new RepoException("Repo name is required");
        }

        List<Repo> repos = getRepoByName(c.getName());
        if (repos.size() != 0) {
            RepoException e = new RepoException("There is already a repo with the name of [" + c.getName() + "]");
            e.setType(RepoException.RepoExceptionType.NAME_ALREADY_EXISTS);
            throw e;
        }
    }

    /**
     * Tests the values of the given repo group to ensure creating the group would be a valid operation, including
     * ensuring the name is specified and there isn't already an existing group with the same name.
     *
     * @param repoGroup group to test
     * @throws RepoException if the group should not be allowed to be created
     */
    private void validateRepoGroup(RepoGroup repoGroup) throws RepoException {
        if (repoGroup.getName() == null || repoGroup.getName().trim().equals("")) {
            throw new RepoException("Repo group name is required");
        }

        RepoGroup existingRepoGroup = getRepoGroupByName(repoGroup.getName());
        if (existingRepoGroup != null) {
            RepoException e = new RepoException("There is already a repo group with the name [" + repoGroup.getName()
                + "]");
            e.setType(RepoException.RepoExceptionType.NAME_ALREADY_EXISTS);
            throw e;
        }
    }

    /**
     * Performs the necessary logic to determine if a candidate repo should be added to the system, adding it
     * in the process if it needs to. If the repo already exists in the system, this method is a no-op.
     * <p/>
     * Calling this method with a repo that has a parent assumes the parent has already been created. This call
     * assumes the repo group has been created as well.
     *
     * @param contentSourceId identifies the content provider that introduced the candidate into the system
     * @param createMe        describes the candidate to be created
     * @throws Exception if there is an error associating the content source with the repo or if the repo
     *                   indicates a parent or repo group that does not exist
     */
    private void addCandidateRepo(int contentSourceId, RepoDetails createMe) throws Exception {

        Subject overlord = subjectManager.getOverlord();
        String name = createMe.getName();

        List<Repo> existingRepo = getRepoByName(name);

        // If the repo doesn't exist, create it.
        if (existingRepo.size() != 0) {
            return;
        }

        // Create and populate the repo
        Repo addMe = new Repo(name);
        addMe.setDescription(createMe.getDescription());

        String createMeGroup = createMe.getRepoGroup();
        if (createMeGroup != null) {
            RepoGroup group = getRepoGroupByName(createMeGroup);
            addMe.addRepoGroup(group);
        }

        // Add the new candidate to the database
        addMe = createCandidateRepo(overlord, addMe);

        // Associate the content provider that introduced the candidate with the repo
        addContentSourcesToRepo(overlord, addMe.getId(), new int[] { contentSourceId });

        // If the repo indicates it has a parent, create that relationship
        String parentName = createMe.getParentRepoName();
        if (parentName != null) {
            List<Repo> parentList = getRepoByName(parentName);

            if (parentList.size() == 0) {
                String error = "Attempting to create repo [" + name + "] with parent [" + parentName
                    + "] but cannot find the parent";
                log.error(error);
                throw new RepoException(error);
            } else {
                Repo parent = parentList.get(0);
                addRepoRelationship(overlord, addMe.getId(), parent.getId(), PARENT_RELATIONSHIP_NAME);
            }
        }
    }

    private void removeRepoFromList(String repoName, List<Repo> repoList) {
        Repo deleteMe = null;
        for (Repo checkMe : repoList) {
            if (checkMe.getName().equals(repoName)) {
                deleteMe = checkMe;
                break;
            }
        }

        if (deleteMe != null) {
            repoList.remove(deleteMe);
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public long getDistributionCountFromRepo(Subject subject, int repoId) {
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, RepoDistribution.QUERY_FIND_BY_REPO_ID);

        countQuery.setParameter("repoId", repoId);

        return ((Long) countQuery.getSingleResult()).longValue();
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @SuppressWarnings("unchecked")
    public PageList<Distribution> findAssociatedDistributions(Subject subject, int repoid, PageControl pc) {

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, RepoDistribution.QUERY_FIND_BY_REPO_ID,
            pc);

        query.setParameter("repoId", repoid);

        List<RepoDistribution> results = query.getResultList();

        ArrayList<Distribution> distros = new ArrayList();
        for (RepoDistribution result : results) {
            distros.add(result.getRepoDistributionPK().getDistribution());

        }
        long count = getDistributionCountFromRepo(subject, repoid);

        return new PageList<Distribution>(distros, (int) count, pc);

    }

    public String calculateSyncStatus(Subject subject, int repoId) {
        Repo found = this.getRepo(subject, repoId);
        Set<ContentSourceSyncStatus> stati = new HashSet<ContentSourceSyncStatus>();
        Set<ContentSource> contentSources = found.getContentSources();
        Iterator<ContentSource> i = contentSources.iterator();
        while (i.hasNext()) {
            ContentSource cs = i.next();
            List<ContentSourceSyncResults> syncResults = cs.getSyncResults();
            // Add the most recent sync results status 
            if (syncResults != null && (!syncResults.isEmpty()) && syncResults.get(0) != null) {
                stati.add(syncResults.get(0).getStatus());
            } else {
                stati.add(ContentSourceSyncStatus.NONE);
            }
        }
        if (stati.contains(ContentSourceSyncStatus.FAILURE)) {
            return ContentSourceSyncStatus.FAILURE.toString();
        }
        if (stati.contains(ContentSourceSyncStatus.INPROGRESS)) {
            return ContentSourceSyncStatus.INPROGRESS.toString();
        }
        if (stati.contains(ContentSourceSyncStatus.SUCCESS)) {
            return ContentSourceSyncStatus.SUCCESS.toString();
        }
        return null;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public int synchronizeRepos(Subject subject, Integer[] repoIds) {
        int syncCount = 0;

        for (Integer id : repoIds) {
            Repo r = this.getRepo(subject, id);
            Set<ContentSource> sources = r.getContentSources();
            Iterator<ContentSource> i = sources.iterator();
            while (i.hasNext()) {
                ContentSource source = i.next();
                contentSourceManager.synchronizeAndLoadContentSource(subject, source.getId());
                syncCount++;
                log.debug("Initiating sync: " + source.getId());
            }
        }

        return syncCount;
    }

}