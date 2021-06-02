package ir.mtn.nwg.controllers;

import ir.mtn.nwg.models.Data;
import ir.mtn.nwg.models.Report;
import ir.mtn.nwg.models.Role;
import ir.mtn.nwg.models.User;
import ir.mtn.nwg.payload.request.LoginRequest;
import ir.mtn.nwg.payload.request.ReportDTO;
import ir.mtn.nwg.payload.request.UserRequest;
import ir.mtn.nwg.payload.request.UserRequest2;
import ir.mtn.nwg.payload.response.JwtResponse;
import ir.mtn.nwg.payload.response.MessageResponse;
import ir.mtn.nwg.repository.DataRepository;
import ir.mtn.nwg.repository.ReportRepository;
import ir.mtn.nwg.repository.RoleRepository;
import ir.mtn.nwg.repository.UserRepository;
import ir.mtn.nwg.security.jwt.JwtUtils;
import ir.mtn.nwg.security.services.UserDetailsImpl;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    private final JwtUtils jwtUtils;

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder encoder;

    private final ReportRepository reportRepository;

    private final DataRepository dataRepository;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils, UserRepository userRepository,
                          RoleRepository roleRepository, PasswordEncoder encoder, ReportRepository reportRepository,
                          DataRepository dataRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.reportRepository = reportRepository;
        this.dataRepository = dataRepository;
    }

    @GetMapping("/sites")
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> getSites() {
        return ResponseEntity.ok(dataRepository.findDistinctSites());
    }

    @GetMapping("/data")
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> getData(@RequestParam String site) {
        if (site != null && !site.isEmpty()) {
            Map<Date, Map<String, Object>> dateData = new HashMap<>();
            dataRepository.findBySite(site).orElse(new ArrayList<>()).stream().forEach(data -> {
                Map<String, Object> dataEntry = dateData.getOrDefault(data.getDate(), new HashMap<>());

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                dataEntry.put("date", simpleDateFormat.format(data.getDate()));
                dataEntry.put(data.getKpi(), data.getValue());

                dateData.put(data.getDate(), dataEntry);
            });
            List<Object> data = dateData.values().stream()
                    .sorted(Comparator.comparing(o -> ((String) o.get("date"))))
                .collect(Collectors.toList());

            List<String> kpis = dataRepository.findDistinctKpisPerSite(site);

            Map<String, Object> result = new HashMap<>();
            result.put("data", data);
            result.put("kpis", kpis);

            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body("Site must be send");
        }
    }

    @GetMapping("/report")
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
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
                userRepository.findAll().stream().forEach(user -> {
                    Map<String, Object> node = recurseNode(null, user);
                    node.put("title", user.getUsername());
                    node.put("key", "user" + user.getId());
                    resultChildren.add(node);
                });
                result.put("children", resultChildren);
                return ResponseEntity.ok(result);
            } else {
                User user = userRepository.findByUsername(((UserDetails) principal).getUsername()).get();
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
        reports.forEach(p -> {
            children.add(recurseNode(p, user));
        });
        node.put("children", children);
        return node;
    }

    @PostMapping("/report")
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> createReport(@Valid @RequestBody ReportDTO report) {
        Report toCreate = new Report();
        toCreate.setName(report.getName());

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(((UserDetails) principal).getUsername()).get();
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

    @PutMapping("/report/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateReport(@PathVariable long id, @Valid @RequestBody ReportDTO report) {
        Report toUpdate = reportRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Error: Wrong ID"));

        toUpdate.setName(report.getName());

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(((UserDetails) principal).getUsername()).get();
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

    @DeleteMapping("/report/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
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




    @PostMapping("/auth/signin")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);
		String jwt = jwtUtils.generateJwtToken(authentication);
		
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		List<String> roles = userDetails.getAuthorities().stream()
				.map(item -> item.getAuthority())
				.collect(Collectors.toList());

		return ResponseEntity.ok(new JwtResponse(jwt, 
												 userDetails.getId(), 
												 userDetails.getUsername(), 
												 userDetails.getEmail(), 
												 roles));
	}




    @GetMapping("/role/{id}")
    public ResponseEntity<Role> getRole(@PathVariable long id) {
        return ResponseEntity.ok(roleRepository.findById(id).get());
    }

    @GetMapping("/role")
    public ResponseEntity<List<Role>> allRoles() {
        return ResponseEntity.ok(roleRepository.findAll());
    }

    @PostMapping("/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createRole(@Valid @RequestBody Role role) {
        if (roleRepository.existsByName(role.getName())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Name already exist!"));
        }
        return ResponseEntity.ok(roleRepository.save(role));
    }

    @DeleteMapping("/role/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteRole(@PathVariable long id) {
        try {
            roleRepository.deleteById(id);
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse(e.getMessage()));
        }
        return ResponseEntity.ok(new MessageResponse("Role deleted successfully!"));
    }




    @GetMapping("/user")
    public ResponseEntity<List<User>> allUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<User> getUser(@PathVariable long id) {
        return ResponseEntity.ok(userRepository.findById(id).get());
    }

    @PostMapping("/user")
    @Secured({"ROLE_ADMIN"})
    public ResponseEntity<?> createUser(@Valid @RequestBody UserRequest userRequest) {
        if (userRepository.existsByUsername(userRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(userRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User(userRequest.getUsername(),
                userRequest.getEmail(),
                encoder.encode(userRequest.getPassword()));

        if (userRequest.getRoles() != null && !userRequest.getRoles().isEmpty()) {
            Set<String> strRoles = new HashSet<>(Arrays.asList(userRequest.getRoles().split(",")));
            Set<Role> roles = new HashSet<>();

            if (strRoles == null) {
                Role userRole = roleRepository.findByName("ROLE_USER")
                        .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                roles.add(userRole);
            } else {
                strRoles.forEach(role -> {
                    Role userRole = roleRepository.findByName(role)
                            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(userRole);
                });
            }
            user.setRoles(roles);
        }

        userRepository.save(user);

        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/user/{id}")
    @Secured({"ROLE_ADMIN"})
    public ResponseEntity<?> deleteUser(@PathVariable long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (!optionalUser.isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: id does not exist!"));
        }

        User user = optionalUser.get();


        userRepository.delete(user);

        return ResponseEntity.ok(new MessageResponse("User deleted successfully!"));
    }

    @PutMapping("/user")
    @Secured({"ROLE_ADMIN"})
    public ResponseEntity<?> updateUser(@Valid @RequestBody UserRequest2 userRequest) {
        Optional<User> optionalUser = userRepository.findByUsername(userRequest.getUsername());
        if (!optionalUser.isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username does not exist!"));
        }

        User user = optionalUser.get();

        if (!user.getEmail().equals(userRequest.getEmail())) {
//            return ResponseEntity
//                    .badRequest()
//                    .body(new MessageResponse("Error: Email can't be changed!"));
            user.setEmail(userRequest.getEmail());
        }

        if (userRequest.getRoles() != null) {
            Set<String> strRoles = new HashSet<>(userRequest.getRoles());
            Set<Role> roles = new HashSet<>();

            if (strRoles == null) {
                Role userRole = roleRepository.findByName("ROLE_USER")
                        .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                roles.add(userRole);
            } else {
                strRoles.forEach(role -> {
                    Role userRole = roleRepository.findByName(role)
                            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(userRole);
                });
            }
            user.setRoles(roles);
        }

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User updated successfully!"));
    }

    @PostMapping("/user/password/{id}")
    @Secured({"ROLE_ADMIN"})
    public ResponseEntity<?> updateUserPassword(@PathVariable long id, @RequestBody String passwordJson) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (!optionalUser.isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Wrong Id"));
        }
        User user = optionalUser.get();

        JSONObject jsonObject = new JSONObject(passwordJson);
        String password = jsonObject.getString("password");
        if (password != null && password.length() > 5 && password.length() < 41) {
            user.setPassword(encoder.encode(password));
            userRepository.save(user);
            return ResponseEntity.ok(new MessageResponse("Password updated successfully!"));
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Password length must be between 6 and 40"));
        }
    }
}
