package com.hzc.secKill.Service;

import com.hzc.secKill.DAO.GoodsDAO;
import com.hzc.secKill.Domain.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoodsService {

    @Autowired
    GoodsDAO goodsDAO;

    public List<GoodsVo> listGoodsVo() {
        return goodsDAO.listGoodsVo();
    }

    public GoodsVo getGoodVoByGoodsId(long goodsId) {
        return goodsDAO.getGoodVoByGoodsId(goodsId);
    }
}
