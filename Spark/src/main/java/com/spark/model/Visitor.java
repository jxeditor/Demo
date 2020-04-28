package com.spark.model;

import com.spark.model.dev.Beijing;
import com.spark.model.dev.Shanghai;
import com.spark.model.dev.Shenzhen;

/**
 * @author XiaShuai on 2020/4/28.
 */
public interface Visitor {
    public void visit(Beijing beijing);

    public void visit(Shanghai shanghai);

    public void visit(Shenzhen shenzhen);
}
