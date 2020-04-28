package com.spark.model.dev;

import com.spark.model.City;
import com.spark.model.Visitor;

/**
 * @author XiaShuai on 2020/4/28.
 */
public class TravelCities implements City {
    City[] cities;

    public TravelCities() {
        cities = new City[]{new Beijing(), new Shanghai(), new Shenzhen()};
    }

    @Override
    public void accept(Visitor visitor) {
        for (int i = 0; i < cities.length; i++) {
            cities[i].accept(visitor);
        }
    }
}
