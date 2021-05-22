package ir.mtn.nwg.repository;

import ir.mtn.nwg.models.Data;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataRepository extends JpaRepository<Data, Long> {
	Optional<List<Data>> findBySite(String site);

	@Query("select distinct site from Data")
	List<String> findDistinctSites();

	@Query("select distinct kpi from Data where site = ?1")
	List<String> findDistinctKpisPerSite(String site);
}
