package com.jev.probe.jev

import com.jev.probe.core.Analysis
import com.jev.probe.core.ChatSnapshot
import com.jev.probe.core.Prefs
import com.jev.probe.core.PackageCatalog
import com.jev.probe.core.kb.ChatContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * The generative route: any OpenAI-compatible `/chat/completions` endpoint.
 * Drafts the 3 candidate replies, and (D stage) summarizes text. Reads
 * replyBaseUrl / replyKey / replyModel from [Prefs].
 */
class ReplyClient(private val prefs: Prefs) {

    /**
     * Exactly 3 varied candidate replies in Chinese.
     *
     * @param ctx D-stage knowledge context. When present its background and
     *        history are prepended to the prompt with an instruction to stay
     *        consistent with them and invent nothing beyond them.
     */
    fun draft(snapshot: ChatSnapshot, relationship: String, ctx: ChatContext? = null, decision: Analysis? = null): List<String> {
        val convo = snapshot.messages.takeLast(10).joinToString("\n") {
            (if (it.side == "me") "我" else "对方") + "：" + it.text
        }
        val sys = "你是芯化和云内部业务员的销售副驾，不是化学品零售客服。业务包括开发采购商、" +
            "挖掘真实询单、匹配供应商、为供应商找下游、筛选实际工厂或贸易商、销售每日询单套餐。" +
            "严格根据 Jev 的客户角色、销售阶段、异议和下一步动作起草三个不同策略的中文微信回复。" +
            "每条不超过100字，每次优先问一个缺失字段，不要连珠炮提问，不要在明确拒绝后反复催促。" +
            "只有真实采购委托才能表示有客户在要货；不能假冒采购商或供应商，也不能虚构询单、" +
            "工厂核验、数据规模、成交截图、合作案例、报价、供应商资质及人工服务结果。" +
            "问数/MCP未接入或未返回已授权且有来源的结果时，不得说已经查到多少家或已有采购商。" +
            "不承诺必然成交、95%准确率、80%转化率或无条件退款；不声称可直接联系任何敏感联系人。" +
            "3880/7880套餐只能引用下面的已确认权益，服务费、积分扣减和退款以现行合同核实。" +
            "如需核验真实工厂、危化品经营与运输、信用或财务情况，提示人工审核。" +
            "只输出JSON数组，含且仅含3条可手动发送的自然口语回复。"
        val decisionBlock = if (decision == null) "" else buildString {
            append("Jev 销售判断（是辅助判断，不是数据库/人工核验结果）：\n")
            append("客户角色：").append(decision.customerRole?.choice ?: "unknown").append('\n')
            append("企业性质（仅为客户描述）：").append(decision.companyType?.choice ?: "unknown").append('\n')
            append("销售阶段：").append(decision.salesStage?.choice ?: "unknown").append('\n')
            append("服务方向：").append(decision.serviceDirection?.choice ?: "unknown").append('\n')
            append("客户当下意图：").append(decision.trueIntent?.choice ?: "unknown").append('\n')
            append("当前异议：").append(decision.objection?.choice ?: "unknown").append('\n')
            append("当前缺失：").append(decision.sheNeeds?.choice ?: "unknown").append('\n')
            append("询盘状态：").append(decision.inquiryReadiness?.choice ?: "unknown").append('\n')
            append("建议动作：").append(decision.bestAction?.choice ?: "unknown").append('\n')
            append("具体事实足够直接回答：").append((decision.shouldReplyNow ?: 0.0) >= 0.5).append('\n')
            append("可讨论套餐（不是付款承诺）：").append((decision.tensionResolved ?: 0.0) >= 0.5).append('\n')
            append("需人工核实：").append((decision.literalQuestion ?: 0.0) >= 0.5).append('\n')
        }
        val user = PackageCatalog.promptContext() + knowledgeBlock(relationship, ctx) + decisionBlock +
            "业务员背景（可编辑）：${relationship}\n\n最近对话：\n${convo}\n\n请给出 3 条候选回复。"
        return parseThree(chat(sys, user, temperature = 0.8))
    }

    /** The background + history preamble; empty string when there is no context. */
    private fun knowledgeBlock(relationship: String, ctx: ChatContext?): String {
        ctx ?: return ""
        val background = ctx.background(relationship)
        val history = ctx.history
        if (background.isBlank() && history.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append("以下是销售背景与业务知识库，回复必须与之一致，")
            .append("只能引用已核验的事实，不得将笔记视为实时价格、库存或安全认证。\n")
        if (background.isNotBlank()) sb.append(background).append('\n')
        if (history.isNotEmpty()) {
            sb.append("\n更早的聊天记录（越靠下越新）：\n")
            history.takeLast(prefs.contextHistoryCount.coerceIn(0, 100)).forEach {
                sb.append(if (it.side == "me") "我：" else "对方：").append(it.text).append('\n')
            }
        }
        sb.append('\n')
        return sb.toString()
    }

    /**
     * One plain chat round trip for the settings connectivity test. Deliberately
     * NOT [summarize]: the test should exercise the ordinary path, not whatever
     * the summary prompt happens to be.
     */
    fun ping(): String =
        chat("你是连通性测试助手，只按要求回答，不要解释。", "请只回复两个字：收到", temperature = 0.0).trim()

    /** Condense a block of text (used by the D-stage contact auto-summary). */
    fun summarize(text: String): String {
        if (text.isBlank()) return ""
        val sys = "你是中文摘要助手。把给到的聊天记录压缩成不超过 120 字的第三人称要点摘要，" +
            "只保留事实、偏好、承诺和待办，不要评论，不要编造。直接输出摘要正文。"
        return chat(sys, text, temperature = 0.2).trim()
    }

    /** One chat-completions round trip; returns the assistant message content. */
    private fun chat(system: String, user: String, temperature: Double): String {
        val url = prefs.replyEndpoint()
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", system))
            .put(JSONObject().put("role", "user").put("content", user))
        val body = JSONObject()
            .put("model", prefs.replyModel)
            .put("messages", messages)
            .put("temperature", temperature)
        val resp = HttpJson.post(url, prefs.effectiveReplyKey(), body, Route.REPLY, HttpJson.headersFor(url))
        return resp.optJSONArray("choices")?.optJSONObject(0)
            ?.optJSONObject("message")?.optString("content") ?: ""
    }

    private fun parseThree(content: String): List<String> {
        val start = content.indexOf('[')
        val end = content.lastIndexOf(']')
        if (start >= 0 && end > start) {
            try {
                val arr = JSONArray(content.substring(start, end + 1))
                val out = ArrayList<String>()
                for (i in 0 until arr.length()) out.add(arr.getString(i).trim())
                if (out.size >= 3) return out.take(3)
                while (out.size < 3) out.add("（稍等，我看下）")
                return out
            } catch (_: Exception) { }
        }
        // Fallback: split lines.
        val lines = content.split("\n").map { it.trim().trimStart('-', '*', '1', '2', '3', '.', ' ', '"') }
            .filter { it.isNotBlank() }
        val out = lines.take(3).toMutableList()
        while (out.size < 3) out.add("（稍等，我看下）")
        return out
    }
}
