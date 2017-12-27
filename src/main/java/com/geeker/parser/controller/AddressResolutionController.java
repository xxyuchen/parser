package com.geeker.parser.controller;

import com.geeker.parser.bean.ConfRegionTable;
import com.geeker.parser.bean.MobileCode;
import com.geeker.parser.dao.AddressParserDao;
import com.geeker.parser.dao.MobileCodeDao;
import org.cloudy.fastjson.JSON;
import org.cloudy.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2017/11/23 0023.
 */
@RestController
public class AddressResolutionController {
    @Value("${amap.api.ip}")
    private String api_ip;

    @Value("${amap.api.regeo}")
    private String api_regeo;

    @Value("${amap.key}")
    private String api_key;

    @Autowired
    private MobileCodeDao mobileCodeDao;

    @Autowired
    private AddressParserDao addressParserDao;

    private RestTemplate restTemplate = new RestTemplate();

    private Logger logger = LoggerFactory.getLogger(AddressResolutionController.class);

    /**
     * 根据ip获取地理位置
     * @param ip
     * @return
     */
    @RequestMapping("/getAddressByIp")
    @ResponseBody
    @Cacheable(value = "ips", key = "'ip_'.concat(#root.args[0])")
    public Map<String,Object> getAddressByIp(String ip){
        Map<String,Object> map = new HashMap<>();
        if(ip.trim().isEmpty()){
            map.put("info","ip地址不能为空！");
            logger.error("------传入空ip------");
            map.put("status",0);
            return map;
        }
        //组织请求信息
        try {
            String result = restTemplate.getForObject(api_ip,String.class,api_key,ip);
            JSONObject json=(JSONObject) JSON.parse(result);
            map.put("info",json.getString("info"));
            map.put("status",json.getInteger("status"));
            if(json.getInteger("status")==0){
                logger.error(json.getString("info"));
                return map;
            }else if (json.getInteger("status")==1){//解析成功
                Map<String,String> areaData = new HashMap<>();
                if(null!=json.getString("province")&&!json.getString("province").isEmpty()){
                    List<ConfRegionTable> province = addressParserDao.findByFullName(json.getString("province"));
                    if(province!=null&&province.size()>0){
                        areaData.put("province",province.get(0).getName());
                    }else{
                        areaData.put("province","");
                    }
                }
                if(null!=json.getString("city")&&!json.getString("city").isEmpty()){
                    List<ConfRegionTable> city = addressParserDao.findByFullName(json.getString("city"));
                    if(city!=null&&city.size()>0){
                        areaData.put("city",city.get(0).getName());
                    }else {
                        areaData.put("city","");
                    }
                }
                areaData.put("adcode",json.getString("adcode"));
                map.put("areaData",areaData);
                logger.info("解析ip："+ip+"成功，地址："+map.get("province")+map.get("city"));
            }
        }catch (Exception e){
            e.printStackTrace();
            logger.error("解析ip：{}失败",ip);
        }
        return map;
    }

    /**
     * 根据经纬度获取地理位置
     * @param location
     * @return
     */
    @RequestMapping("/getAddressByItude")
    @ResponseBody
    @Cacheable(value = "locations", key = "'location_'.concat(#root.args[0])")
    public Map<String,Object> getAddressByItude(String location){
        Map<String,Object> map = new HashMap<>();
        if(location.trim().isEmpty()){
            map.put("info","经纬度不能为空！");
            logger.error("------传入空经纬度------");
            map.put("status",0);
            return map;
        }
        //组织请求信息
        try {
            String result = restTemplate.getForObject(api_regeo,String.class,api_key,location);
            JSONObject json=(JSONObject) JSON.parse(result);
            map.put("info",json.getString("info"));
            map.put("status",json.getInteger("status"));
            if(json.getInteger("status")==0){
                logger.error(json.getString("info"));
                return map;
            }else if (json.getInteger("status")==1){
                JSONObject regeocode = (JSONObject) JSON.parse(json.getString("regeocode"));
                JSONObject addressComponent = (JSONObject) JSON.parse(regeocode.getString("addressComponent"));
                Map<String,String> areaData = new HashMap<>();
                if(null!=addressComponent.getString("province")&&!addressComponent.getString("province").isEmpty()){
                    List<ConfRegionTable> province = addressParserDao.findByFullName(addressComponent.getString("province"));
                    if(province!=null&&province.size()>0){
                        areaData.put("province",province.get(0).getName());
                    }else {
                        areaData.put("province","");
                    }
                }
                logger.info(addressComponent.getString("city"));
                if(null!=addressComponent.getString("city")&&!addressComponent.getString("city").isEmpty()){
                    List<ConfRegionTable> city = addressParserDao.findByFullName(addressComponent.getString("city"));
                    if(city!=null&&city.size()>0){
                        areaData.put("city",city.get(0).getName());
                    }else {
                        areaData.put("city","");
                    }
                }
                areaData.put("country",addressComponent.getString("country"));
                areaData.put("cityCode",addressComponent.getString("citycode"));
                areaData.put("district",addressComponent.getString("district"));
                areaData.put("adcode",addressComponent.getString("adcode"));
                areaData.put("township",addressComponent.getString("township"));
                areaData.put("towncode",addressComponent.getString("towncode"));
                map.put("areaData",areaData);
                logger.info("解析经纬度："+location+"成功，地址：{}",regeocode.getString("formatted_address"));
            }
        }catch (Exception e){
            e.printStackTrace();
            logger.error("解析经纬度：{}失败",location);
        }
        return map;
    }

    /**
     * 根据手机号码解析地址
     * @param mobile
     * @return
     */
    @RequestMapping("/getAddressByMobile")
    @ResponseBody
    @Cacheable(value = "mobiles", key = "'mobile_'.concat(#root.args[0].substring(0,7))")
    public Map<String,Object> getAddressByMobile(String mobile){
        Map<String,Object> map = new HashMap<>();
        if(mobile.trim().isEmpty()){
            map.put("info","号段不能为空！");
            logger.error("------传入空号段------");
            map.put("status",0);
            return map;
        }
        try {
            MobileCode mobileCode = mobileCodeDao.findByMobile(mobile.substring(0,7));
            if(mobileCode==null){
                map.put("info","该号段不存在！");
                map.put("status",0);
                return map;
            }
            map.put("info","OK");
            map.put("status",1);
            Map<String,String> mobileData = new HashMap<>();
            mobileData.put("province",mobileCode.getProvince());
            mobileData.put("city",mobileCode.getCity());
            mobileData.put("areaCode",mobileCode.getAreaCode());
            mobileData.put("postCode",mobileCode.getPostCode());
            map.put("areaData",mobileData);
            logger.error("解析{}号段：{}成功",mobileCode.getCorp(),mobile);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("解析号段：{}失败",mobile);
        }
        return map;
    }

}
