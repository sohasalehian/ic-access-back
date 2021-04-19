package ir.mtn.nwg.controllers;

import ir.mtn.nwg.models.Role;
import ir.mtn.nwg.models.User;
import ir.mtn.nwg.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/role")
public class RoleController {

}
