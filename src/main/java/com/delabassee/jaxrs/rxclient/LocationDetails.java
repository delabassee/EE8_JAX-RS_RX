package com.delabassee.jaxrs.rxclient;

import java.time.LocalTime;
import java.util.Optional;


/**
 *
 * @author davidd
 */
public class LocationDetails {

    private String city;
    private String country;
    private LocalTime start;
    private LocalTime end;

    public Optional<String> get() {        
        if (city != null && country != null)
            return Optional.of(city + " (" + country + ")");            
        else 
            return Optional.empty();

    }

    public String getCity() {
        return city;
    }

    public void setCity(final String city) {
        System.out.println("City set : " + city + "!!!");
        this.city = city.replace("\"", "");
    }

    public String getCountry() {
        return country;
    }
    
    public void setStart(final LocalTime start) {
        this.start = start;
    }

    public void setCountry(final String country) {
        System.out.println("Country set : " + country + "!!!");
        this.country = country.replace("\"", "");
    }
    
    
}

