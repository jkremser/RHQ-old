package org.rhq.core.domain.test;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.unitils.UnitilsTestNG;
import org.unitils.dbunit.annotation.DataSet;
import org.unitils.orm.jpa.annotation.JpaEntityManagerFactory;

@JpaEntityManagerFactory(persistenceUnit = "rhq-test", configFile = "META-INF/test-persistence.xml")
@DataSet
public abstract class JPATest extends UnitilsTestNG {

    @PersistenceContext
    protected EntityManager entityMgr;

    protected EntityManager getEntityManager() {
        return entityMgr;
    }

}
