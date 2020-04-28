package com.spark.model.dev;

import com.spark.model.City;
import com.spark.model.Visitor;

/**
 * @author XiaShuai on 2020/4/28.
 */
public class Shanghai implements City {
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
