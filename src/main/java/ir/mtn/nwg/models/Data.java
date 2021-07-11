package ir.mtn.nwg.models;

import ir.mtn.nwg.enums.MoEntity;
import ir.mtn.nwg.enums.MoView;
import ir.mtn.nwg.enums.TimePeriod;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(
    name = "data",
    uniqueConstraints = {
        @UniqueConstraint(name = "UniqueNumberAndStatus",
                          columnNames = { "date", "time_period", "element", "kpi" })
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Data {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private Long id;

    @Column(name = "date", nullable = false)
    private Date date;

    @Enumerated(EnumType.STRING)
    @Column(name = "mo_entity")
    private MoEntity moEntity = MoEntity.NOKIA_2G;

    @Enumerated(EnumType.STRING)
    @Column(name = "mo_view")
    private MoView moView = MoView.SITE;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_period")
    private TimePeriod timePeriod = TimePeriod.DAILY;

    @Column(name = "element")
    private String element;

    @Column(name = "kpi")
    private String kpi;

    @Column(name = "value")
    private Double value;

}
