package org.rhq.core.domain.content;

import java.io.Serializable;
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

@Entity
@NamedQueries( {
    @NamedQuery(name = Advisory.QUERY_FIND_ALL, query = "SELECT adv FROM Advisory adv"),
    @NamedQuery(name = Advisory.QUERY_FIND_BY_ADV, query = "SELECT adv " + "  FROM Advisory adv "
        + " WHERE adv.advisory = :advisory "),
    @NamedQuery(name = Advisory.QUERY_DELETE_BY_ADV_ID, query = "DELETE Advisory adv WHERE adv.id = :advid"),
    @NamedQuery(name = Advisory.QUERY_FIND_BY_ADV_ID, query = "SELECT adv FROM Advisory adv WHERE adv.id = :id "),
    @NamedQuery(name = Advisory.QUERY_FIND_COMPOSITE_BY_ID, query = "SELECT new org.rhq.core.domain.content.composite.AdvisoryDetailsComposite( "
        + "          a, "
        + "          a.advisory, "
        + "          a.advisoryType,"
        + "          a.topic,"
        + "          a.synopsis,"
        + "          a.description,"
        + "          a.solution,"
        + "          a.severity,"
        + "          a.update_date,"
        + "          a.issue_date"
        + "       ) "
        + "  FROM Advisory a "
        + "  WHERE a.id = :id "),
    @NamedQuery(name = Advisory.DELETE_IF_NO_PACKAGES, query = "" //
        + "DELETE Advisory adv " //
        + "WHERE adv.advisorypkgs IS EMPTY"
    )
})
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_ADVISORY_ID_SEQ")
@Table(name = "RHQ_ADVISORY")
public class Advisory implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String QUERY_FIND_ALL = "Advisory.findAll";
    public static final String QUERY_FIND_BY_ADV = "Advisory.findByAdv";
    public static final String QUERY_DELETE_BY_ADV_ID = "Advisory.deleteByAdvId";
    public static final String QUERY_FIND_COMPOSITE_BY_ID = "Advisory.queryFindCompositeByAdvId";
    public static final String QUERY_FIND_BY_ADV_ID = "Advisory.queryFindByAdvId";

    public static final String DELETE_IF_NO_PACKAGES = "Advisory.deleteIfNoPackages";

    // Attributes  --------------------------------------------

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "ADVISORY", nullable = false)
    private String advisory;

    @Column(name = "ADVISORY_TYPE", nullable = false)
    private String advisoryType;

    @Column(name = "ADVISORY_REL", nullable = true)
    private String advisory_rel;

    @Column(name = "ADVISORY_NAME", nullable = true)
    private String advisory_name;

    @Column(name = "DESCRIPTION", nullable = true)
    private String description;

    @Column(name = "SYNOPSIS", nullable = false)
    private String synopsis;

    @Column(name = "TOPIC", nullable = true)
    private String topic;

    @Column(name = "SOLUTION", nullable = true)
    private String solution;

    @Column(name = "SEVERITY", nullable = true)
    private String severity;

    @Column(name = "ISSUE_DATE", nullable = true)
    private long issue_date;

    @Column(name = "UPDATE_DATE", nullable = true)
    private long update_date;

    @Column(name = "CTIME", nullable = true)
    private long ctime;

    @Column(name = "LAST_MODIFIED", nullable = true)
    private long lastModifiedDate;

    @OneToMany(mappedBy = "advisory", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<AdvisoryPackage> advisorypkgs;

    @OneToMany(mappedBy = "advisory", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<AdvisoryBuglist> advisorybugs;

    // Constructor ----------------------------------------

    public Advisory() {
    }

    public Advisory(String advisory, String advisoryType, String synopsis) {
        setAdvisory(advisory);
        setAdvisoryType(advisoryType);
        setSynopsis(synopsis);

    }

    public String getAdvisory() {
        return advisory;
    }

    public void setAdvisory(String advisory) {
        this.advisory = advisory;
    }

    public String getAdvisoryType() {
        return advisoryType;
    }

    public void setAdvisoryType(String advisoryType) {
        this.advisoryType = advisoryType;
    }

    public String getAdvisory_rel() {
        return advisory_rel;
    }

    public void setAdvisory_rel(String advisoryRel) {
        advisory_rel = advisoryRel;
    }

    public String getAdvisory_name() {
        return advisory_name;
    }

    public void setAdvisory_name(String advisoryName) {
        advisory_name = advisoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public long getIssue_date() {
        return issue_date;
    }

    public void setIssue_date(long issueDate) {
        issue_date = issueDate;
    }

    public long getUpdate_date() {
        return update_date;
    }

    public void setUpdate_date(long updateDate) {
        update_date = updateDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setLastModifiedDate(long lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public long getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Set<AdvisoryPackage> getAdvisorypkgs() {
        return advisorypkgs;
    }

    public void setAdvisorypkgs(Set<AdvisoryPackage> advisorypkgs) {
        this.advisorypkgs = advisorypkgs;
    }

    public Set<AdvisoryBuglist> getAdvisorybugs() {
        return advisorybugs;
    }

    public void setAdvisorybugs(Set<AdvisoryBuglist> advisorybugs) {
        this.advisorybugs = advisorybugs;
    }

    // Object Overridden Methods  --------------------------------------------

    @Override
    public String toString() {
        return String.format("Advisory [Advisory=%s, Type=%s, Name=%s]", advisory, advisoryType, advisory_name);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!(o instanceof Advisory)) {
            return false;
        }

        Advisory adv = (Advisory) o;

        if ((getAdvisory() != null) ? (!getAdvisory().equals(adv.getAdvisory())) : (adv.getAdvisory() != null)) {
            return false;
        }

        return true;
    }

    @PrePersist
    void onPersist() {
        this.setLastModifiedDate(System.currentTimeMillis());
    }

    @PreUpdate
    void onUpdate() {
        this.setLastModifiedDate(System.currentTimeMillis());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((getAdvisory() == null) ? 0 : getAdvisory().hashCode());
        return result;
    }

}