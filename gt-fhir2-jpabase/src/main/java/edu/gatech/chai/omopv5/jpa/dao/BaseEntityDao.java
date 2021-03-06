package edu.gatech.chai.omopv5.jpa.dao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import edu.gatech.chai.omopv5.jpa.entity.BaseEntity;

@Repository
public abstract class BaseEntityDao<T extends BaseEntity> implements IDao<T> {
	@PersistenceContext
	private EntityManager em;

	public EntityManager getEntityManager() {
		return em;
	}
	
	public void add(T baseEntity) {
		em.persist(baseEntity);
	}
	
	public T findById(Class<T> entityClass, Long id) {
		return em.find(entityClass, id);
	}
	
	public void merge(T baseEntity) {
		em.merge(baseEntity);
	}

	public void rollback() {
		em.getTransaction().rollback();
	}

	@Override
	public Long delete(Class<T> entityClass, Long id) {
		T entity = findById(entityClass, id);
		if (entity != null) {
			em.remove(entity);
			return id;
		} else {
			return 0L;
		}
	}
	
}
