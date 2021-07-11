package ir.mtn.nwg.controllers;

import ir.mtn.nwg.repository.DataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
public class DataController {

    private final DataRepository dataRepository;

    @Autowired
    public DataController(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @GetMapping("/data")
    public ResponseEntity<?> getData(@RequestParam String element,
                                     @RequestParam(required = false) String moEntity,
                                     @RequestParam(required = false) String moView,
//                                     @RequestParam(required = false) String[] kpis,
                                     @RequestParam(required = false) Integer[] specificHours,
                                     @RequestParam(required = false) String timePeriod,
                                     @RequestParam(required = false) Date from,
                                     @RequestParam(required = false) Date to) {
        if (element != null && !element.isEmpty()) {
            Map<Date, Map<String, Object>> dateData = new HashMap<>();
            dataRepository.findByElement(element).orElse(new ArrayList<>()).forEach(data -> {
                Map<String, Object> dataEntry = dateData.getOrDefault(data.getDate(), new HashMap<>());

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                dataEntry.put("date", simpleDateFormat.format(data.getDate()));
                dataEntry.put(data.getKpi(), data.getValue());

                dateData.put(data.getDate(), dataEntry);
            });
            List<Object> data = dateData.values().stream()
                    .sorted(Comparator.comparing(o -> ((String) o.get("date"))))
                .collect(Collectors.toList());

            List<String> kpis = dataRepository.findDistinctKpisPerElement(element);

            Map<String, Object> result = new HashMap<>();
            result.put("data", data);
            result.put("kpis", kpis);

            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body("Error: missing parameter");
        }
    }
}
