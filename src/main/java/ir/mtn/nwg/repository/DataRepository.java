package ir.mtn.nwg.repository;

import ir.mtn.nwg.enums.MoEntity;
import ir.mtn.nwg.enums.MoView;
import ir.mtn.nwg.models.Data;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataRepository extends JpaRepository<Data, Long> {
	Optional<List<Data>> findByElement(String element);

	@Query("SELECT DISTINCT element FROM Data WHERE moEntity=?1 AND moView=?2")
	List<String> findDistinctElementsByMoEntityAndMoView(MoEntity moEntity, MoView moView);

	@Query("SELECT DISTINCT kpi FROM Data WHERE moEntity=?1 AND moView=?2")
	List<String> findDistinctKpisByMoEntityAndMoView(MoEntity moEntity, MoView moView);

	@Query("SELECT DISTINCT kpi FROM Data WHERE element = ?1")
	List<String> findDistinctKpisPerElement(String element);
}
