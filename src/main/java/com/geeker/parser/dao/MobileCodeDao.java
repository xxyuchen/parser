package com.geeker.parser.dao;

import com.geeker.parser.bean.MobileCode;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by Administrator on 2017/11/27 0027.
 */
public interface MobileCodeDao extends JpaRepository<MobileCode,Integer> {
    /**
     * 根据号段查询位置
     * @return
     */
    MobileCode findByMobile(String mobile);

}
