package com.geeker.parser.dao;

import com.geeker.parser.bean.ConfRegionTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by Administrator on 2017/12/15 0015.
 */
public interface AddressParserDao extends JpaRepository<ConfRegionTable,Integer> {
    /**
     * 修正地理信息
     * @param fullName
     * @return
     */
    List<ConfRegionTable> findByFullName(String fullName);
}
