package com.icbc.lingmou.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.lingmou.entity.Branch;
import com.icbc.lingmou.mapper.BranchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 应用启动时自动注入 4 个网点种子数据
 * 幂等：已存在 branch_code 的网点不会重复插入
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final BranchMapper branchMapper;

    @Override
    public void run(String... args) {
        List<BranchSeed> seeds = Arrays.asList(
                new BranchSeed("BJ01", "工商银行北京西单支行", "北京市西城区西单北大街120号"),
                new BranchSeed("SH01", "工商银行上海浦东支行", "上海市浦东新区世纪大道100号"),
                new BranchSeed("SZ01", "工商银行深圳南山支行", "深圳市南山区科技园南区高新南一道"),
                new BranchSeed("HZ01", "工商银行杭州西湖支行", "杭州市西湖区曙光路128号")
        );

        int inserted = 0;
        for (BranchSeed s : seeds) {
            Long count = branchMapper.selectCount(
                    new LambdaQueryWrapper<Branch>().eq(Branch::getBranchCode, s.code));
            if (count == 0) {
                Branch b = new Branch();
                b.setBranchCode(s.code);
                b.setName(s.name);
                b.setAddress(s.address);
                b.setBusinessHours("09:00-17:00");
                b.setCurrentQueue(0);
                b.setBusyLevel("IDLE");
                branchMapper.insert(b);
                inserted++;
                log.info("[种子数据] 插入网点 {} ({})", s.code, s.name);
            }
        }
        if (inserted > 0) {
            log.info("[种子数据] 本次共插入 {} 条网点记录", inserted);
        } else {
            log.info("[种子数据] 4 个网点已存在，跳过");
        }
    }

    private record BranchSeed(String code, String name, String address) {}
}
