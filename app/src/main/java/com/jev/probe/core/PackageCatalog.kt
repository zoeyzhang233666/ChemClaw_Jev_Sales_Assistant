package com.jev.probe.core

/**
 * 芯化和云每日询单套餐。Values from the two user-provided package cards.
 * Annual inquiry counts are ENTITLEMENTS, not a guaranteed number of orders or
 * a claim that all received inquiries stop at the quota. Contract governs use.
 */
data class ServicePackage(
    val label: String,
    val priceYuan: Int,
    val durationMonths: Int,
    val inquiryQuota: Int,
    val featuredProducts: Int,
    val subscribedProducts: Int,
    val expertMatches: Int,
    val certifiedBadge: Boolean,
    val consulting: Boolean,
    val pointsRedemption: Boolean,
    val supplyChainFinance: Boolean
)

object PackageCatalog {
    val BASIC = ServicePackage(
        label = "每日询单企业基础版",
        priceYuan = 3880, durationMonths = 12,
        inquiryQuota = 45, featuredProducts = 10,
        subscribedProducts = 50, expertMatches = 10,
        certifiedBadge = false, consulting = true,
        pointsRedemption = true, supplyChainFinance = false
    )
    val PRO = ServicePackage(
        label = "每日询单企业专业版",
        priceYuan = 7880, durationMonths = 12,
        inquiryQuota = 90, featuredProducts = 30,
        subscribedProducts = 200, expertMatches = 25,
        certifiedBadge = true, consulting = true,
        pointsRedemption = true, supplyChainFinance = true
    )

    /** Used only as approved, static product facts for reply drafting. */
    fun promptContext(): String = buildString {
        append("芯化和云当前提供的每日询单企业套餐（按套餐权益卡，实际交易以当前合同为准）：\n")
        for (p in listOf(BASIC, PRO)) {
            append(p.label).append("：").append(p.priceYuan).append("元，服务")
                .append(p.durationMonths).append("个月；意向询单额度")
                .append(p.inquiryQuota).append("条，主打产品")
                .append(p.featuredProducts).append("个，订阅产品")
                .append(p.subscribedProducts).append("个，专家撮合服务")
                .append(p.expertMatches).append("次；企业认证专属标签")
                .append(if (p.certifiedBadge) "有" else "无")
                .append("，行业顾问咨询")
                .append(if (p.consulting) "有" else "无")
                .append("，积分兑现")
                .append(if (p.pointsRedemption) "有" else "无")
                .append("，供应链金融")
                .append(if (p.supplyChainFinance) "有" else "无").append("。\n")
        }
        append("基础版价格是3880，不是3888。45/90条为一年可获取的经过筛选的意向询单额度，")
        append("不是保证成交45/90单。不得擅自承诺免费、保证收益、100%工厂、全额退款、")
        append("服务费计算规则或积分等于人民币；具体扣费、超额、到期退费及服务规则核对现行合同。\n")
    }
}
