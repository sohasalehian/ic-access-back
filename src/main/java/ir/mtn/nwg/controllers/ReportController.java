package ir.mtn.nwg.controllers;

import ir.mtn.nwg.models.Report;
import ir.mtn.nwg.models.User;
import ir.mtn.nwg.payload.request.ReportDTO;
import ir.mtn.nwg.payload.response.MessageResponse;
import ir.mtn.nwg.repository.ReportRepository;
import ir.mtn.nwg.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/report")
@PreAuthorize("hasRole('USER')  or hasRole('MANAGER') or hasRole('ADMIN')")
public class ReportController {

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;

    @Autowired
    public ReportController(UserRepository userRepository,
                            ReportRepository reportRepository) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
    }

    @GetMapping("")
    public ResponseEntity<?> getReports() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        boolean isAdmin = false;
        if (principal instanceof UserDetails) {
            for (GrantedAuthority grantedAuthority : ((UserDetails) principal).getAuthorities()) {
                if (grantedAuthority.getAuthority().equals("ROLE_ADMIN")) {
                    isAdmin = true;
                    break;
                }
            }

            if (isAdmin) {
                Map<String, Object> result = new HashMap<>();
                List<Object> resultChildren = new LinkedList<>();
                userRepository.findAll().forEach(user -> {
                    Map<String, Object> node = recurseNode(null, user);
                    node.put("title", user.getUsername());
                    node.put("key", "user" + user.getId());
                    resultChildren.add(node);
                });
                result.put("children", resultChildren);
                return ResponseEntity.ok(result);
            } else {
                User user = userRepository.findByUsername(((UserDetails) principal).getUsername())
                        .orElseThrow(() -> new RuntimeException("Error: Wrong username"));
                return ResponseEntity.ok(recurseNode(null, user));
            }
        }
        return ResponseEntity.ok('O');
    }
    private Map<String, Object> recurseNode (Report report, User user) {
        Map<String, Object> node = new HashMap<>();
        if (report != null) {
            node.put("title", report.getName());
            node.put("key", "report" + report.getId());
            node.put("id", report.getId());
        }
        List<Object> children = new LinkedList<>();

        List<Report> reports = reportRepository.findByParentAndUser(report, user).orElse(new ArrayList<>());
        if (reports.isEmpty() && report != null && report.getLink() != null && !report.getLink().isEmpty()) {
            node.put("isLeaf", true);
            node.put("link", report.getLink());
        }
        reports.forEach(p -> children.add(recurseNode(p, user)));
        node.put("children", children);
        return node;
    }

    @PostMapping("")
    public ResponseEntity<?> createReport(@Valid @RequestBody ReportDTO report) {
        Report toCreate = new Report();
        toCreate.setName(report.getName());

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(((UserDetails) principal).getUsername())
                .orElseThrow(() -> new RuntimeException("Error: Wrong username"));
        toCreate.setUser(user);

        if (report.getParent() != null) {
            Report parent = reportRepository.findById(report.getParent())
                    .orElseThrow(() -> new RuntimeException("Error: Wrong Parent"));
            if (!parent.getUser().equals(user)) {
                throw new RuntimeException("Error: Wrong Parent");
            }
            toCreate.setParent(parent);
        }

        if (report.getLink() != null && !report.getLink().isEmpty()) {
            toCreate.setLink(report.getLink());
        }

        return ResponseEntity.ok(reportRepository.save(toCreate) + " Report created successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateReport(@PathVariable long id, @Valid @RequestBody ReportDTO report) {
        Report toUpdate = reportRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Error: Wrong ID"));

        toUpdate.setName(report.getName());

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(((UserDetails) principal).getUsername()).
                orElseThrow(() -> new RuntimeException("Error: Wrong username"));
        if(!toUpdate.getUser().equals(user)) {
            throw new RuntimeException("Error: Not Owner");
        }

        if (report.getLink() != null && !report.getLink().isEmpty()) {
            if (toUpdate.getLink() != null && !toUpdate.getLink().isEmpty()) {
                toUpdate.setLink(report.getLink());
            } else {
                throw new RuntimeException("Can't change Category to Report");
            }
        }

        if (toUpdate.getLink() != null && !toUpdate.getLink().isEmpty()) {
            if (report.getLink() == null || report.getLink().isEmpty()) {
                throw new RuntimeException("Can't change Report to Category");
            }
        }

        return ResponseEntity.ok(reportRepository.save(toUpdate) + " Report updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReport(@PathVariable long id) {
        try {
            reportRepository.deleteById(id);
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse(e.getMessage()));
        }
        return ResponseEntity.ok(new MessageResponse("Report deleted successfully!"));
    }
}
