package com.pgconnect.property;
import jakarta.validation.constraints.NotBlank;import jakarta.validation.constraints.Size;import org.springframework.web.bind.annotation.*;import java.util.List;
@RestController
public class GeocodingController{
 private final GeocodingService geocoder;public GeocodingController(GeocodingService geocoder){this.geocoder=geocoder;}
 @GetMapping("/api/public/locations/resolve") public List<GeocodingService.PlaceCandidate> resolve(@RequestParam @NotBlank @Size(max=160) String query){return geocoder.resolve(query);}
}
