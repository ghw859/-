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
 * 应用启动时自动注入 6 个北京网点种子数据
 * 逐字对齐前端 branch.ts 的 6 个网点
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
                new BranchSeed("b1", "北京分行营业部",         "busy",     850,  "[\"大额现金\",\"外汇\",\"无障碍\"]", 25, 42, 18, "8/10", "[18,22,28,20,25,25]", "北京市西城区复兴门内大街55号",              "010-66695588", "09:00 - 17:00", "fa-solid fa-landmark",   "bg-blue-50",    "text-blue-600"),
                new BranchSeed("b2", "长安街智慧示范支行",     "moderate", 1400, "[\"自助发卡\",\"VTM\"]",           8, 19,  7, "5/6",  "[12,10,6,9,7,8]",    "北京市东城区东长安街1号",                  "010-65129588", "09:00 - 17:00", "fa-solid fa-robot",      "bg-cyan-50",    "text-cyan-600"),
                new BranchSeed("b3", "金融街私人银行旗舰支行", "free",     2100, "[\"VIP\"]",                       3,  8,  3, "6/6",  "[5,4,3,2,3,3]",      "北京市西城区金融大街15号",                  "010-66299588", "09:00 - 17:30", "fa-solid fa-crown",      "bg-emerald-50", "text-emerald-600"),
                new BranchSeed("b4", "中关村科技创新特色支行", "moderate", 3800, "[\"对公\"]",                      12, 25, 12, "6/8",  "[8,15,10,14,11,12]", "北京市海淀区中关村大街22号",                "010-62599588", "09:00 - 17:00", "fa-solid fa-microchip",  "bg-purple-50", "text-purple-600"),
                new BranchSeed("b5", "望京SOHO社区支行",       "free",     3200, "[\"自助发卡\",\"无障碍\"]",          5, 10,  4, "4/4",  "[8,6,4,7,5,5]",      "北京市朝阳区望京街10号望京SOHO塔1座",       "010-59799588", "09:00 - 17:00", "fa-solid fa-shop",       "bg-emerald-50", "text-emerald-600"),
                new BranchSeed("b6", "国贸CBD中心支行",        "busy",     1800, "[\"外汇\",\"VIP\",\"对公\"]",          20, 38, 15, "7/8",  "[15,18,22,17,20,20]", "北京市朝阳区建国门外大街1号国贸大厦",         "010-65059588", "09:00 - 17:00", "fa-solid fa-city",       "bg-blue-50",    "text-blue-600")
        );

        int inserted = 0;
        for (BranchSeed s : seeds) {
            Long count = branchMapper.selectCount(
                    new LambdaQueryWrapper<Branch>().eq(Branch::getBranchCode, s.code));
            if (count == 0) {
                Branch b = new Branch();
                b.setBranchCode(s.code);
                b.setName(s.name);
                b.setBusyLevel(s.busyLevel);
                b.setDistance(s.distance);
                b.setServices(s.services);
                b.setWaitTime(s.waitTime);
                b.setFlowCount(s.flowCount);
                b.setReserveCount(s.reserveCount);
                b.setWindowInfo(s.windowInfo);
                b.setTrend(s.trend);
                b.setAddress(s.address);
                b.setPhone(s.phone);
                b.setHours(s.hours);
                b.setIcon(s.icon);
                b.setIconBg(s.iconBg);
                b.setIconColor(s.iconColor);
                b.setFavorite(0);
                branchMapper.insert(b);
                inserted++;
                log.info("[种子数据] 插入网点 {} ({})", s.code, s.name);
            }
        }
        if (inserted > 0) {
            log.info("[种子数据] 本次共插入 {} 条网点记录", inserted);
        } else {
            log.info("[种子数据] 6 个网点已存在，跳过");
        }
    }

    private record BranchSeed(
            String code, String name, String busyLevel, int distance,
            String services, int waitTime, int flowCount, int reserveCount,
            String windowInfo, String trend, String address, String phone,
            String hours, String icon, String iconBg, String iconColor
    ) {}
}
