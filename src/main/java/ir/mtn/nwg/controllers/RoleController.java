package ir.mtn.nwg.controllers;

import ir.mtn.nwg.models.Role;
import ir.mtn.nwg.payload.response.MessageResponse;
import ir.mtn.nwg.repository.RoleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/role")
@Secured({"ROLE_ADMIN"})
public class RoleController {

    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Role> getRole(@PathVariable long id) {
        return ResponseEntity.ok(roleRepository.findById(id)
                .<RuntimeException>orElseThrow(() -> new RuntimeException("Error: Role is not found.")));
    }

    @GetMapping("")
    public ResponseEntity<List<Role>> allRoles() {
        return ResponseEntity.ok(roleRepository.findAll());
    }

    @PostMapping("")
    public ResponseEntity<?> createRole(@Valid @RequestBody Role role) {
        if (roleRepository.existsByName(role.getName())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Name already exist!"));
        }
        return ResponseEntity.ok(roleRepository.save(role));
    }

    @DeleteMapping("/{id}")
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
}
