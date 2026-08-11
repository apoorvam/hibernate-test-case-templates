package org.hibernate.bugs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Envers maps the bidirectional collection-change revision of Parent from the object
 * reference held by the child's collection, without unwrapping Hibernate proxies
 * (BaseEnversCollectionEventListener#generateBidirectionalCollectionChangeWorkUnits).
 *
 * When the session already holds an uninitialized Parent proxy (seeded here by Child's
 * lazy @ManyToOne during em.find), the collection element resolves to that proxy, and
 * the property mapper's reflection field-reads on the proxy wrapper return null for
 * every audited property. The resulting PARENT_AUD MOD row is all NULL - including the
 * @Version column when org.hibernate.envers.do_not_audit_optimistic_locking_field=false.
 *
 * Contrast with the entity-event path, which unwraps correctly
 * (BaseEnversEventListener#addCollectionChangeWorkUnit, HHH-7249).
 */
class JPAUnitTestCase {

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void init() {
		entityManagerFactory = Persistence.createEntityManagerFactory( "templatePU" );
	}

	@AfterEach
	void destroy() {
		entityManagerFactory.close();
	}

	@Test
	void collectionChangeRevisionKeepsParentData() throws Exception {
		Long parentId;
		Long childId;

		// tx 1: parent referenced by the child twice - lazy @ManyToOne and owned @ManyToMany
		EntityManager em = entityManagerFactory.createEntityManager();
		em.getTransaction().begin();
		Parent parent = new Parent( "parent" );
		Child child = new Child( "child" );
		child.setMainParent( parent );
		child.getLinkedParents().add( parent );
		em.persist( parent );
		em.persist( child );
		em.getTransaction().commit();
		parentId = parent.getId();
		childId = child.getId();
		em.close();

		// tx 2: loading the child hydrates mainParent as an UNINITIALIZED Parent proxy;
		// deleting the child fires the collection-remove event on linkedParents, whose
		// element resolves to that proxy
		em = entityManagerFactory.createEntityManager();
		em.getTransaction().begin();
		em.remove( em.find( Child.class, childId ) );
		em.getTransaction().commit();
		em.close();

		// the collection change generated a MOD revision for Parent - verify its content
		em = entityManagerFactory.createEntityManager();
		try {
			AuditReader auditReader = AuditReaderFactory.get( em );
			List<Number> revisions = auditReader.getRevisions( Parent.class, parentId );
			assertEquals( 2, revisions.size(), "expected ADD + collection-change MOD revisions" );

			Parent atModRevision = auditReader.find( Parent.class, parentId, revisions.get( revisions.size() - 1 ) );
			assertNotNull( atModRevision );

			// FAILS: null - the MOD revision recorded NULL for every audited property
			assertEquals( "parent", atModRevision.getName(),
					"collection-change revision must preserve the parent's data" );
			// FAILS: null - violates NOT NULL version columns on databases that enforce it
			assertNotNull( atModRevision.getVersion(),
					"collection-change revision must record the parent's version" );
		}
		finally {
			em.close();
		}
	}
}
