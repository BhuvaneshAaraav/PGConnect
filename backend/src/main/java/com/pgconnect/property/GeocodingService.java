package com.pgconnect.property;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeocodingService {
    private final String apiKey;
    private final RestClient client=RestClient.builder().baseUrl("https://maps.googleapis.com/maps/api/geocode/json").build();
    public GeocodingService(@Value("${pgconnect.maps.api-key:}") String apiKey){this.apiKey=apiKey;}
    public List<PlaceCandidate> resolve(String query){
        if(apiKey.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Place search needs a configured Google Maps geocoding API key. Search by locality text or use your device location instead.");
        try{
            JsonNode response=client.get().uri(uri->uri.queryParam("address",query.trim()).queryParam("key",apiKey).build()).retrieve().body(JsonNode.class);
            String status=response.path("status").asText();
            if(!"OK".equals(status)&&!"ZERO_RESULTS".equals(status))throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"The place search provider could not resolve that location. Try a more specific college or locality name.");
            List<PlaceCandidate> result=new ArrayList<>();for(JsonNode item:response.path("results")){
                JsonNode location=item.path("geometry").path("location");if(!location.path("lat").isNumber()||!location.path("lng").isNumber())continue;
                result.add(new PlaceCandidate(item.path("place_id").asText(),item.path("formatted_address").asText(),location.path("lat").asDouble(),location.path("lng").asDouble()));
                if(result.size()==5)break;
            }return List.copyOf(result);
        }catch(ResponseStatusException e){throw e;}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"The place search provider is unavailable. Search by locality text or use your device location instead.");}
    }
    public record PlaceCandidate(String placeId,String formattedAddress,double latitude,double longitude){}
}
