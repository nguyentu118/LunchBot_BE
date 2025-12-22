package vn.codegym.lunchbot_be.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GhnServiceRequest {

    @JsonProperty("shop_id")
    private Integer shopId;

    @JsonProperty("from_district")
    private Integer fromDistrict;

    @JsonProperty("to_district")
    private Integer toDistrict;
}
