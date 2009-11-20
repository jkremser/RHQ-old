/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.content;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.rhq.core.domain.common.Tag;
import org.rhq.core.domain.common.Taggable;
import org.rhq.core.domain.resource.Resource;

/**
 * A repo represents a set of related {@link PackageVersion}s. The packages in this repo are populated by its
 * {@link ContentSource}s. The relationship with content sources is weak; that is, content sources can come and go, even
 * as the packages contained in the repo remain.
 *
 * @author Jason Dobies
 * @author John Mazzitelli
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = Repo.QUERY_FIND_ALL_IMPORTED_REPOS, query = "SELECT c FROM Repo c WHERE c.candidate = false"),
    @NamedQuery(name = Repo.QUERY_FIND_BY_IDS, query = "SELECT c FROM Repo c WHERE c.id IN ( :ids )"),
    @NamedQuery(name = Repo.QUERY_FIND_BY_NAME, query = "SELECT c FROM Repo c WHERE c.name = :name"),
    @NamedQuery(name = Repo.QUERY_FIND_IMPORTED_BY_CONTENT_SOURCE_ID_FETCH_CCS, query = "SELECT c FROM Repo c LEFT JOIN FETCH c.repoContentSources ccs WHERE ccs.contentSource.id = :id AND c.candidate = false"),
    @NamedQuery(name = Repo.QUERY_FIND_IMPORTED_BY_CONTENT_SOURCE_ID, query = "SELECT c FROM Repo c LEFT JOIN c.repoContentSources ccs WHERE ccs.contentSource.id = :id AND c.candidate = false"),
    @NamedQuery(name = Repo.QUERY_FIND_CANDIDATE_BY_CONTENT_SOURCE_ID, query = "SELECT c FROM Repo c LEFT JOIN c.repoContentSources ccs WHERE ccs.contentSource.id = :id AND c.candidate = true"),
    @NamedQuery(name = Repo.QUERY_FIND_SUBSCRIBER_RESOURCES, query = "SELECT rc.resource FROM ResourceRepo rc WHERE rc.repo.id = :id"),
    @NamedQuery(name = Repo.QUERY_FIND_REPOS_BY_RESOURCE_ID, query = "SELECT c "
        + "FROM ResourceRepo rc JOIN rc.repo c WHERE rc.resource.id = :resourceId "),

    @NamedQuery(name = Repo.QUERY_FIND_REPO_COMPOSITES_BY_RESOURCE_ID, query = "SELECT new org.rhq.core.domain.content.composite.RepoComposite( "
        + "c, "
        + "(SELECT COUNT(cpv.packageVersion) FROM RepoPackageVersion cpv WHERE cpv.repo.id = c.id) "
        + ") "
        + "FROM ResourceRepo rc JOIN rc.repo c LEFT JOIN c.repoPackageVersions pv "
        + "WHERE rc.resource.id = :resourceId "
        + "GROUP BY c, c.name, c.description, c.creationDate, c.lastModifiedDate"),
    @NamedQuery(name = Repo.QUERY_FIND_REPO_COMPOSITES_BY_RESOURCE_ID_COUNT, query = "SELECT COUNT( rc.repo ) "
        + "FROM ResourceRepo rc WHERE rc.resource.id = :resourceId "),

    @NamedQuery(name = Repo.QUERY_FIND_AVAILABLE_REPO_COMPOSITES_BY_RESOURCE_ID, query = "SELECT new org.rhq.core.domain.content.composite.RepoComposite( "
        + "c, "
        + "(SELECT COUNT(cpv.packageVersion) FROM RepoPackageVersion cpv WHERE cpv.repo.id = c.id) "
        + ") "
        + "FROM Repo AS c "
        + "WHERE c.id NOT IN ( SELECT rc.repo.id FROM ResourceRepo rc WHERE rc.resource.id = :resourceId ) "
        + "AND c.candidate is false " + "GROUP BY c, c.name, c.description, c.creationDate, c.lastModifiedDate"),
    @NamedQuery(name = Repo.QUERY_FIND_AVAILABLE_REPO_COMPOSITES_BY_RESOURCE_ID_COUNT, query = "SELECT COUNT( c ) "
        + "FROM Repo AS c "
        + "WHERE c.id NOT IN ( SELECT rc.repo.id FROM ResourceRepo rc WHERE rc.resource.id = :resourceId ) "
        + "AND c.candidate = false "),
    @NamedQuery(name = Repo.QUERY_FIND_CANDIDATES_WITH_ONLY_CONTENT_SOURCE, query = "SELECT r FROM Repo r " //
        + "WHERE r.candidate = true " //
        + "AND (SELECT COUNT(rcs.repo) FROM RepoContentSource rcs " //
        + "     WHERE rcs.repo.id = r.id " //
        + "     AND rcs.contentSource.id = :contentSourceId) = 1 " //
        + "AND (SELECT COUNT(rcs) FROM RepoContentSource rcs" //
        + "     WHERE rcs.repo.id = r.id) = 1 ")
    })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_REPO_ID_SEQ")
@Table(name = "RHQ_REPO")
public class Repo implements Serializable, Taggable {

    // Constants  --------------------------------------------

    public static final String QUERY_FIND_ALL_IMPORTED_REPOS = "Repo.findAll";
    public static final String QUERY_FIND_BY_IDS = "Repo.findByIds";
    public static final String QUERY_FIND_BY_NAME = "Repo.findByName";
    public static final String QUERY_FIND_IMPORTED_BY_CONTENT_SOURCE_ID_FETCH_CCS = "Repo.findByContentSourceIdFetchCCS";
    public static final String QUERY_FIND_IMPORTED_BY_CONTENT_SOURCE_ID = "Repo.findByContentSourceId";
    public static final String QUERY_FIND_CANDIDATE_BY_CONTENT_SOURCE_ID = "Repo.findCandidateByContentSourceId";
    public static final String QUERY_FIND_SUBSCRIBER_RESOURCES = "Repo.findSubscriberResources";
    public static final String QUERY_FIND_REPOS_BY_RESOURCE_ID = "Repo.findReposByResourceId";
    public static final String QUERY_FIND_REPO_COMPOSITES_BY_RESOURCE_ID = "Repo.findRepoCompositesByResourceId";
    public static final String QUERY_FIND_REPO_COMPOSITES_BY_RESOURCE_ID_COUNT = "Repo.findRepoCompositesByResourceId_count";
    public static final String QUERY_FIND_AVAILABLE_REPO_COMPOSITES_BY_RESOURCE_ID = "Repo.findAvailableRepoCompositesByResourceId";
    public static final String QUERY_FIND_AVAILABLE_REPO_COMPOSITES_BY_RESOURCE_ID_COUNT = "Repo.findAvailableRepoCompositesByResourceId_count";
    public static final String QUERY_FIND_CANDIDATES_WITH_ONLY_CONTENT_SOURCE = "Repo.findCandidatesWithOnlyContentSource";

    private static final long serialVersionUID = 1L;

    // Attributes  --------------------------------------------

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", nullable = true)
    private String description;

    @Column(name = "CREATION_TIME", nullable = false)
    private long creationDate;

    @Column(name = "LAST_MODIFIED_TIME", nullable = false)
    private long lastModifiedDate;

    @Column(name = "IS_CANDIDATE", nullable = false)
    private boolean candidate;

    @OneToMany(mappedBy = "repo", fetch = FetchType.LAZY)
    private Set<ResourceRepo> resourceRepos;

    @OneToMany(mappedBy = "repo", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<RepoContentSource> repoContentSources;

    @OneToMany(mappedBy = "repo", fetch = FetchType.LAZY)
    private Set<RepoPackageVersion> repoPackageVersions;

    @OneToMany(mappedBy = "repo", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<RepoRepoGroup> repoRepoGroups;

    @OneToMany(mappedBy = "repo", fetch = FetchType.LAZY)
    private Set<RepoRepoRelationship> repoRepoRelationships;

    @OneToMany(mappedBy = "repo", fetch = FetchType.LAZY)
    private Set<RepoTag> repoTags;

    @Transient
    private String syncStatus;

    // Constructor ----------------------------------------

    public Repo() {
        // for JPA use
    }

    public Repo(String name) {
        this.name = name;
    }

    // Public  --------------------------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Programmatic name of the repo.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * User specified description of the repo.
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Timestamp of when this repo was created.
     */
    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Timestamp of the last time the {@link #getContentSources() sources} of this repo was changed. It is not
     * necessarily the last time any other part of this repo object was changed (for example, this last modified date
     * does not necessarily correspond to the time when the description was modified).
     */
    public long getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(long lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * Indicates if the repo is a "real" repo in RHQ or is just a candidate for import, such as those introduced by
     * a content provider.
     */
    public boolean isCandidate() {
        return candidate;
    }

    public void setCandidate(boolean candidate) {
        this.candidate = candidate;
    }

    /**
     * Returns the explicit mapping entities.
     *
     * @return the mapping entities
     *
     * @see    #getResources()
     */
    public Set<ResourceRepo> getResourceRepos() {
        return resourceRepos;
    }

    /**
     * The resources subscribed to this repo.
     *
     * <p>The returned set is not backed by this entity - if you want to alter the set of associated resources, use
     * {@link #getResourceRepos()} or {@link #addResource(Resource)}, {@link #removeResource(Resource)}.</p>
     */
    public Set<Resource> getResources() {
        HashSet<Resource> resources = new HashSet<Resource>();

        if (resourceRepos != null) {
            for (ResourceRepo rc : resourceRepos) {
                resources.add(rc.getResourceRepoPK().getResource());
            }
        }

        return resources;
    }

    /**
     * Directly subscribe a resource to this repo.
     *
     * @param  resource
     *
     * @return the mapping that was added
     */
    public ResourceRepo addResource(Resource resource) {
        if (this.resourceRepos == null) {
            this.resourceRepos = new HashSet<ResourceRepo>();
        }

        ResourceRepo mapping = new ResourceRepo(resource, this);
        this.resourceRepos.add(mapping);
        return mapping;
    }

    /**
     * Unsubscribes the resource from this repo, if it exists. If it was already subscribed, the mapping that was
     * removed is returned; if not, <code>null</code> is returned.
     *
     * @param  resource the resource to unsubscribe from this repo
     *
     * @return the mapping that was removed or <code>null</code> if the resource was not subscribed to this repo
     */
    public ResourceRepo removeResource(Resource resource) {
        if ((this.resourceRepos == null) || (resource == null)) {
            return null;
        }

        ResourceRepo doomed = null;

        for (ResourceRepo rc : this.resourceRepos) {
            if (resource.equals(rc.getResourceRepoPK().getResource())) {
                doomed = rc;
                break;
            }
        }

        if (doomed != null) {
            this.resourceRepos.remove(doomed);
        }

        return doomed;
    }

    /**
     * Returns the explicit mapping entities.
     *
     * @return the mapping entities
     *
     * @see    #getContentSources()
     */
    public Set<RepoContentSource> getRepoContentSources() {
        return repoContentSources;
    }

    /**
     * Get the overall sync status of this Repository.  This is a summation of all the syncs.
     * 
     * There is a weight to the status since this returns the most 'relevant' status:
     * 
     * 1) ContentSourceSyncStatus.FAILURE
     * 2) ContentSourceSyncStatus.INPROGRESS
     * 3) ContentSourceSyncStatus.SUCCESS
     * 
     * @return String summary of the status of this Repository
     */
    @Transient
    public String getSyncStatus() {
        return this.syncStatus;
    }

    /**
     * The content sources that this repo serves up. These are the content sources that provide or provided packages
     * for this repo. This relationship is weak; a content source may not be in this set but the packages it loaded
     * into this repo may still exist.
     *
     * <p>The returned set is not backed by this entity - if you want to alter the set of associated content sources,
     * use {@link #getRepoContentSources()} or {@link #addContentSource(ContentSource)},
     * {@link #removeContentSource(ContentSource)}.</p>
     */
    public Set<ContentSource> getContentSources() {
        HashSet<ContentSource> contentSources = new HashSet<ContentSource>();

        if (repoContentSources != null) {
            for (RepoContentSource ccs : repoContentSources) {
                contentSources.add(ccs.getRepoContentSourcePK().getContentSource());
            }
        }

        return contentSources;
    }

    /**
     * Directly assign a content source to this repo.
     *
     * @param  contentSource
     *
     * @return the mapping that was added
     */
    public RepoContentSource addContentSource(ContentSource contentSource) {
        if (this.repoContentSources == null) {
            this.repoContentSources = new HashSet<RepoContentSource>();
        }

        RepoContentSource mapping = new RepoContentSource(this, contentSource);
        this.repoContentSources.add(mapping);
        return mapping;
    }

    /**
     * Removes the content source from this repo, if it exists. If it does exist, the mapping that was removed is
     * returned; if the given content source did not exist as one that is a member of this repo, <code>null</code> is
     * returned.
     *
     * @param  contentSource the content source to remove from this repo
     *
     * @return the mapping that was removed or <code>null</code> if the content source was not mapped to this repo
     */
    public RepoContentSource removeContentSource(ContentSource contentSource) {
        if ((this.repoContentSources == null) || (contentSource == null)) {
            return null;
        }

        RepoContentSource doomed = null;

        for (RepoContentSource ccs : this.repoContentSources) {
            if (contentSource.equals(ccs.getRepoContentSourcePK().getContentSource())) {
                doomed = ccs;
                break;
            }
        }

        if (doomed != null) {
            this.repoContentSources.remove(doomed);
        }

        return doomed;
    }

    /**
     * Returns the explicit mapping entities.
     *
     * @return the mapping entities
     *
     * @see    #getPackageVersions()
     */
    public Set<RepoPackageVersion> getRepoPackageVersions() {
        return repoPackageVersions;
    }

    /**
     * The package versions that this repo serves up. Subscribers to this repo will have access to the returned
     * set of package versions. These are package versions that were directly assigned to the repo and those that
     * were assigned via its relationship with its content sources. This is the relationship that should be consulted
     * when determining what package versions this repo exposes - do not look at the indirect relationship from
     * content sources to package versions. When content sources are assigned to this repo, this package version
     * relationship will be automatically managed.
     *
     * <p>The returned set is not backed by this entity - if you want to alter the set of associated package versions,
     * use {@link #getRepoPackageVersions()} or {@link #addPackageVersion(PackageVersion)},
     * {@link #removePackageVersion(PackageVersion)}.</p>
     */
    public Set<PackageVersion> getPackageVersions() {
        HashSet<PackageVersion> packageVersions = new HashSet<PackageVersion>();

        if (repoPackageVersions != null) {
            for (RepoPackageVersion cpv : repoPackageVersions) {
                packageVersions.add(cpv.getRepoPackageVersionPK().getPackageVersion());
            }
        }

        return packageVersions;
    }

    /**
     * Directly assign a package version to this repo.
     *
     * @param  packageVersion
     *
     * @return the mapping that was added
     */
    public RepoPackageVersion addPackageVersion(PackageVersion packageVersion) {
        if (this.repoPackageVersions == null) {
            this.repoPackageVersions = new HashSet<RepoPackageVersion>();
        }

        RepoPackageVersion mapping = new RepoPackageVersion(this, packageVersion);
        this.repoPackageVersions.add(mapping);
        return mapping;
    }

    /**
     * Removes the package version from this repo, if it exists. If it does exist, the mapping that was removed is
     * returned; if the given package version did not exist as one that is a member of this repo, <code>null</code>
     * is returned.
     *
     * @param  packageVersion the package version to remove from this repo
     *
     * @return the mapping that was removed or <code>null</code> if the package version was not mapped to this repo
     */
    public RepoPackageVersion removePackageVersion(PackageVersion packageVersion) {
        if ((this.repoPackageVersions == null) || (packageVersion == null)) {
            return null;
        }

        RepoPackageVersion doomed = null;

        for (RepoPackageVersion cpv : this.repoPackageVersions) {
            if (packageVersion.equals(cpv.getRepoPackageVersionPK().getPackageVersion())) {
                doomed = cpv;
                break;
            }
        }

        if (doomed != null) {
            this.repoPackageVersions.remove(doomed);
        }

        return doomed;
    }

    /**
     * Returns the explicit mapping entities.
     *
     * @return the mapping entities
     *
     * @see    #getRepoGroups()
     */
    public Set<RepoRepoGroup> getRepoRepoGroups() {
        return repoRepoGroups;
    }

    /**
     * The repogroups that this repo belongs to.
     *
     * <p>The returned set is not backed by this entity - if you want to alter
     * the set of associated repogroups,
     * use {@link #getRepoRepoGroups()} or {@link #addRepoGroup(RepoGroup)},
     * {@link #removeRepoGroup(RepoGroup)}.</p>
     */
    public Set<RepoGroup> getRepoGroups() {
        HashSet<RepoGroup> repoGroups = new HashSet<RepoGroup>();

        if (repoRepoGroups != null) {
            for (RepoRepoGroup rrg : repoRepoGroups) {
                repoGroups.add(rrg.getRepoRepoGroupPK().getRepoGroup());
            }
        }

        return repoGroups;
    }

    /**
     * Directly assign a repogroup to this repo.
     *
     * @param  repoGroup
     *
     * @return the mapping that was added
     */
    public RepoRepoGroup addRepoGroup(RepoGroup repoGroup) {
        if (this.repoRepoGroups == null) {
            this.repoRepoGroups = new HashSet<RepoRepoGroup>();
        }

        RepoRepoGroup mapping = new RepoRepoGroup(this, repoGroup);
        this.repoRepoGroups.add(mapping);
        return mapping;
    }

    /**
     * Removes association with a repo group, if it exists. If it does exist, the mapping that was removed is
     * returned; if the given repo group did not exist as one that is a associated to this repo, <code>null</code> is
     * returned.
     *
     * @param  repoGroup the repo group to disassociate from this repo
     *
     * @return the mapping that was removed or <code>null</code> if the repo group was not associated with this repo
     */
    public RepoRepoGroup removeRepoGroup(RepoGroup repoGroup) {
        if ((this.repoRepoGroups == null) || (repoGroup == null)) {
            return null;
        }

        RepoRepoGroup doomed = null;

        for (RepoRepoGroup rrg : this.repoRepoGroups) {
            if (repoGroup.equals(rrg.getRepoRepoGroupPK().getRepoGroup())) {
                doomed = rrg;
                break;
            }
        }

        if (doomed != null) {
            this.repoRepoGroups.remove(doomed);
        }

        return doomed;
    }

    /**
     * Returns the explicit mapping entities.
     *
     * @return the mapping entities
     *
     * @see    #getRepoRelationships()
     */
    public Set<RepoRepoRelationship> getRepoRepoRelationships() {
        return repoRepoRelationships;
    }

    /**
     * The reporelationships that this repo belongs to.
     *
     * <p>The returned set is not backed by this entity - if you want to alter
     * the set of associated repoRelationships,
     * use {@link #getRepoRepoRelationships()} or {@link #addRepoRelationship(RepoRelationship)},
     * {@link #removeRepoRelationship(RepoRelationship)}.</p>
     */
    public Set<RepoRelationship> getRepoRelationships() {
        HashSet<RepoRelationship> repoRelationships = new HashSet<RepoRelationship>();

        if (repoRepoRelationships != null) {
            for (RepoRepoRelationship rrr : repoRepoRelationships) {
                repoRelationships.add(rrr.getRepoRepoRelationshipPK().getRepoRelationship());
            }
        }

        return repoRelationships;
    }

    /**
     * Directly assign a reporelationship to this repo.
     *
     * @param  repoRelationship
     *
     * @return the mapping that was added
     */
    public RepoRepoRelationship addRepoRelationship(RepoRelationship repoRelationship) {
        if (this.repoRepoRelationships == null) {
            this.repoRepoRelationships = new HashSet<RepoRepoRelationship>();
        }

        RepoRepoRelationship mapping = new RepoRepoRelationship(this, repoRelationship);
        this.repoRepoRelationships.add(mapping);
        return mapping;
    }

    /**
     * Removes association with a repo relationship, if it exists. If it does exist, the mapping that was removed is
     * returned; if the given repo relationship did not exist as one that is a associated to this repo, <code>null</code> is
     * returned.
     *
     * @param  repoRelationship the repo relationship to disassociate from this repo
     *
     * @return the mapping that was removed or <code>null</code> if the repo relationship was not associated with this repo
     */
    public RepoRepoRelationship removeRepoRelationship(RepoRelationship repoRelationship) {
        if ((this.repoRepoRelationships == null) || (repoRelationship == null)) {
            return null;
        }

        RepoRepoRelationship doomed = null;

        for (RepoRepoRelationship rrr : this.repoRepoRelationships) {
            if (repoRelationship.equals(rrr.getRepoRepoRelationshipPK().getRepoRelationship())) {
                doomed = rrr;
                break;
            }
        }

        if (doomed != null) {
            this.repoRepoRelationships.remove(doomed);
        }

        return doomed;
    }

    @Override
    public String toString() {
        return "Repo: id=[" + this.id + "], name=[" + this.name + "]";
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof Repo))) {
            return false;
        }

        final Repo other = (Repo) obj;

        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }

        return true;
    }

    @PrePersist
    void onPersist() {
        this.lastModifiedDate = this.creationDate = System.currentTimeMillis();
    }

    @PreUpdate
    void onUpdate() {
        this.lastModifiedDate = System.currentTimeMillis();
    }

    public Set<RepoTag> getRepoTags() {
        return repoTags;
    }

    public void setRepoTags(Set<RepoTag> tags) {
        this.repoTags = repoTags;
    }

    public boolean hasTag(Tag tag) {
        if ((this.repoTags == null) || (tag == null)) {
            return false;
        }

        for (RepoTag rt : this.repoTags) {
            if (tag.equals(rt.getRepoTagPK().getTag())) {
                return true;
            }
        }
        return false;

    }

    /**
     * The tags associated with this repo.
     */
    public Set<Tag> getTags() {
        HashSet<Tag> tags = new HashSet<Tag>();

        if (repoTags != null) {
            for (RepoTag rt : repoTags) {
                tags.add(rt.getRepoTagPK().getTag());
            }
        }

        return tags;
    }

    /**
     * Add a tag association with this repo.
     *
     * @param  tag
     */
    public void addTag(Tag tag) {
        if (this.repoTags == null) {
            this.repoTags = new HashSet<RepoTag>();
        }

        RepoTag mapping = new RepoTag(this, tag);
        this.repoTags.add(mapping);
    }

    /**
     * Set tag associations with this repo.
     *
     * @param  tag
     */
    public void setTags(Set<Tag> tags) {
        if (this.repoTags == null) {
            this.repoTags = new HashSet<RepoTag>();
        } else {
            this.repoTags.clear();
        }

        for (Tag t : tags) {
            RepoTag mapping = new RepoTag(this, t);
            this.repoTags.add(mapping);
        }

    }

    /**
     * Removes association with a tag, if it exists. 
     */
    public void removeTag(Tag tag) {
        if ((this.repoTags == null) || (tag == null)) {
            return;
        }

        RepoTag doomed = null;

        for (RepoTag rt : this.repoTags) {
            if (tag.equals(rt.getRepoTagPK().getTag())) {
                doomed = rt;
                break;
            }
        }

        if (doomed != null) {
            this.repoTags.remove(doomed);
        }
    }

    /**
     * Set the sync status for this repo.
     * @param syncStatusIn
     */
    @Transient
    public void setSyncStatus(String syncStatusIn) {
        this.syncStatus = syncStatusIn;
    }
}
