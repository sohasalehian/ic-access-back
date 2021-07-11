package ir.mtn.nwg.controllers;

import ir.mtn.nwg.enums.MoEntity;
import ir.mtn.nwg.enums.MoView;
import ir.mtn.nwg.repository.DataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('USER')  or hasRole('MANAGER') or hasRole('ADMIN')")
public class MoController {

    private final DataRepository dataRepository;

    @Autowired
    public MoController(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @GetMapping("/mo-entity")
     public ResponseEntity<?> getEntities() {
        return ResponseEntity.ok(MoEntity.values());
    }

    @GetMapping("/mo-view")
    public ResponseEntity<?> getViews() {
        return ResponseEntity.ok(MoView.values());
    }

    @GetMapping("/mo-element")
    public ResponseEntity<?> getNames(@RequestParam(required = false) String moEntity,
                                      @RequestParam(required = false) String moView) {
        return ResponseEntity.ok(dataRepository.findDistinctElementsByMoEntityAndMoView(
                MoEntity.valueOf(moEntity), MoView.valueOf(moView)));
    }

    @GetMapping("/kpis")
    public ResponseEntity<?> getKpis(@RequestParam(required = false) String moEntity,
                                      @RequestParam(required = false) String moView) {
        return ResponseEntity.ok(dataRepository.findDistinctKpisByMoEntityAndMoView(
                MoEntity.valueOf(moEntity), MoView.valueOf(moView)));
    }
}
