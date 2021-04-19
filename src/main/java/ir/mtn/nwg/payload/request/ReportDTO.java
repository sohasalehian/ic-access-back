package ir.mtn.nwg.payload.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class ReportDTO {

    private Long parent;

    @NotBlank
    @Size(max = 50)
    private String name;

    @Size(max = 254)
    private String link;

}
