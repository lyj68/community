package com.mycompany.community.dao;

import org.springframework.stereotype.Repository;

//区分Alpha接口的实现类
@Repository("alphaHiternate")

public class AlphaDaoHibernateImpl implements AlphaDao {
    @Override
    public String select() {
        return "Hibernate";
    }
}
