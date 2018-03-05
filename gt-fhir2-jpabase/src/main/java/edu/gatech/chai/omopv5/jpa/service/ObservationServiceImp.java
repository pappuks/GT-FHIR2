package edu.gatech.chai.omopv5.jpa.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.gatech.chai.omopv5.jpa.dao.ObservationDao;
import edu.gatech.chai.omopv5.jpa.entity.Measurement;
import edu.gatech.chai.omopv5.jpa.entity.Observation;

@Service
public class ObservationServiceImp implements ObservationService {

	@Autowired
	private ObservationDao observationDao;
	
	@Transactional(readOnly = true)
	@Override
	public Observation findById(Long id) {
		return observationDao.findById(id);
	}

	@Transactional(readOnly = true)
	@Override
	public List<Observation> searchByColumnString(String column, String value) {
		EntityManager em = observationDao.getEntityManager();
		
		String query = "SELECT t FROM Observation t WHERE "+column+" like :value";
		List<Observation> results = em.createQuery(query, Observation.class)
				.setParameter("value",  value).getResultList();
		return results;	
	}

	@Override
	public List<Observation> searchWithoutParams(int fromIndex, int toIndex) {
		int length = toIndex - fromIndex;
		EntityManager em = observationDao.getEntityManager();
		
		String query = "SELECT p FROM Observation p ORDER BY id ASC";
		List<Observation> retvals = (List<Observation>) em.createQuery(query, Observation.class)
				.setFirstResult(fromIndex)
				.setMaxResults(length)
				.getResultList();
		
		return retvals;
	}

	@Override
	public List<Observation> searchWithParams(int fromIndex, int toIndex,
			Map<String, List<ParameterWrapper>> paramMap) {
		int length = toIndex - fromIndex;
		EntityManager em = observationDao.getEntityManager();
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Observation> query = builder.createQuery(Observation.class);
		Root<Observation> root = query.from(Observation.class);
		
		List<Observation> retvals = new ArrayList<Observation>();
		List<Predicate> predicates = ParameterWrapper.constructPredicate(builder, paramMap, root);		
		if (predicates == null || predicates.isEmpty()) return retvals; // Nothing. return empty list
	
		query.select(root);
		query.where(builder.and(predicates.toArray(new Predicate[predicates.size()])));

		if (length <= 0) {
			retvals = em.createQuery(query)
					.getResultList();
		} else {
			retvals = em.createQuery(query)
					.setFirstResult(fromIndex)
					.setMaxResults(length)
					.getResultList();
		}
		return retvals;
	}

	@Override
	public Observation createOrUpdate(Observation entity) {
		if (entity.getId() != null) {
			observationDao.merge(entity);
		} else {
			observationDao.add(entity);
		}
		return entity;
	}

	@Override
	public Long getSize() {
		EntityManager em = observationDao.getEntityManager();
		
		String query = "SELECT COUNT(p) FROM Observation p";
		Long totalSize = em.createQuery(query, Long.class).getSingleResult();
		return totalSize;
	}

	@Override
	public Long getSize(Map<String, List<ParameterWrapper>> paramMap) {
		// Construct predicate from this map.
		EntityManager em = observationDao.getEntityManager();
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Long> observationQuery = builder.createQuery(Long.class);
		Root<Observation> observationRoot = observationQuery.from(Observation.class);

		List<Predicate> predicates = ParameterWrapper.constructPredicate(builder, paramMap, observationRoot);		
		if (predicates == null || predicates.isEmpty()) return 0L;

		observationQuery.select(builder.count(observationRoot));
		observationQuery.where(builder.and(predicates.toArray(new Predicate[predicates.size()])));
		
		return em.createQuery(observationQuery).getSingleResult();
	}

}
