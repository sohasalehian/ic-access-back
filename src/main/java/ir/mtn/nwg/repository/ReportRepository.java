package ir.mtn.nwg.repository;

import ir.mtn.nwg.models.Report;
import ir.mtn.nwg.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
	List<Report> findByParentAndUser(Report parent, User user);
}
