package com.jev.probe.jev

import com.jev.probe.core.ChatSnapshot
import com.jev.probe.core.kb.LogEntry
import org.json.JSONArray
import org.json.JSONObject

/**
 * Jev decision question set for 芯化和云 buyer development and package sales. Instructions/criteria in English; chat text
 * stays Chinese. The state `from` field uses "me"/"other" (the instructions
 * already refer to "the other person" throughout).
 */
object JevQuestions {

    /**
     * Appended to every question so the D-stage `background` field (relationship,
     * contact notes, knowledge-base hits) reads as given context rather than as
     * an off-topic digression that should be penalized.
     */
    const val BACKGROUND_NOTE =
        " Facts given in background are provided context, not off-topic."

    private fun noul(instructions: String, t: String, f: String) = JSONObject().apply {
        put("type", "noul")
        put("instructions", instructions + BACKGROUND_NOTE)
        put("criteria", JSONObject().put("true", t).put("false", f))
    }

    private fun choice(instructions: String, criteria: Map<String, String>) = JSONObject().apply {
        put("type", "choice")
        put("instructions", instructions + BACKGROUND_NOTE)
        put("criteria", JSONObject().also { c -> criteria.forEach { (k, v) -> c.put(k, v) } })
    }

    private fun score(instructions: String, levels: List<String>) = JSONObject().apply {
        put("type", "score")
        put("instructions", instructions + BACKGROUND_NOTE)
        put("criteria", JSONArray().also { a -> levels.forEach { a.put(it) } })
    }

    /**
     * 芯化和云销售副驾: both BUYER inquiry development and SUPPLIER service sales.
     * The seven legacy answer keys are kept for UI/API compatibility; extra
     * questions provide explicit role, stage, service direction and objections.
     * Chat messages are evidence, not proof of purchase intent or factory status.
     */
    fun judge(): JSONObject = JSONObject().apply {
        put("customer_role", choice(
            "Classify the contact's CURRENT business role, based on explicit evidence; a producer may also buy inputs. " +
                "Never classify factory versus trader as verified solely from chat or a business registration.",
            linkedMapOf(
                "buyer" to "Seeking to BUY raw materials or supplies for own use or resale.",
                "supplier" to "Offering or producing chemicals and seeking downstream buyers.",
                "both" to "Explicitly has both buying and selling / sourcing needs in the conversation.",
                "unknown" to "Role not sufficiently clear."
            )
        ))
        put("company_type", choice(
            "What company type does the chat CLAIM or indicate? Do not claim verified factory identity.",
            linkedMapOf(
                "claimed_factory" to "Contact says their company manufactures or operates a production plant; NOT verified.",
                "claimed_trader" to "Contact says their company trades or distributes; NOT verified.",
                "mixed" to "Explicitly says they both manufacture and trade.",
                "unknown" to "No reliable statement in the supplied evidence."
            )
        ))
        put("sales_stage", choice(
            "Which stage of 芯化和云 sales/service conversation is evidenced NOW? Do not assume a sale happened.",
            linkedMapOf(
                "prospecting" to "First contact, business identity or products not yet established.",
                "discovery" to "Asking about supplied products, needed raw materials, quantity, region or customer goals.",
                "trust_building" to "Showing examples or explaining verified data, sourcing or manual screening process.",
                "package_discussion" to "Customer asks about 3880/7880 services, benefits, subscription or charges.",
                "objection" to "Customer expresses a concern, rejection, bad prior experience or counterargument.",
                "contract_payment" to "Contract review/signing, payment timing, payment confirmation or deposit.",
                "delivery_followup" to "Ongoing member service, inquiry delivery, fulfillment, refund or complaint.",
                "unknown" to "No adequate evidence of a stage."
            )
        ))
        put("service_direction", choice(
            "Which 芯化和云 task does the customer actually want? Do not conflate supplier search and downstream leads.",
            linkedMapOf(
                "find_downstream" to "Supplier seeks downstream buyers, plants, customer list or sales opportunities.",
                "find_supplier" to "Buyer seeks raw materials, suppliers, quotations or sourcing assistance.",
                "verify_factory" to "Needs manual check whether a business is an actual factory or trader.",
                "inquiry_matching" to "Wants real purchase inquiries or demand notifications.",
                "transaction_match" to "Wants help negotiating or matching a specific supply-demand deal.",
                "package_rights" to "Wants to understand service tiers, quotas, points or contract terms.",
                "other" to "Unclear or unrelated."
            )
        ))
        put("objection", choice(
            "Identify the customer's explicit CURRENT objection, if any; do not infer hidden motives.",
            linkedMapOf(
                "data_accuracy" to "Questions company list accuracy, verified factories, data freshness or contact quality.",
                "price" to "Thinks service price or fee too high.",
                "effectiveness" to "Asks whether inquiries, leads or matching will lead to real business.",
                "trust" to "Fears service deteriorates after signing or doubts service provider reliability.",
                "duplicates" to "Already owns data, worries leads resold widely or duplicates competitors.",
                "platform" to "Doesn't want another promotion platform or compares with other platforms.",
                "rights_refund" to "Questions inquiry count, point consumption, fees, refunds, expiry or contract.",
                "no_need" to "Clearly says no current demand, no time or no interest.",
                "other" to "Another explicit concern.",
                "none" to "No stated objection."
            )
        ))
        // Legacy keys mapped to 芯化和云 sales decisions.
        put("true_intent", choice(
            "Identify latest directly evidenced customer intent. This is NOT a guess at purchase probability.",
            linkedMapOf(
                "identify_business" to "Clarifying business, main products, buyer or supplier identity.",
                "explore_leads" to "Asks about downstream companies, customer data, factories or market opportunities.",
                "request_sourcing" to "Asks for suppliers, quotations, raw-material availability or procurement help.",
                "provide_inquiry" to "Provides a concrete purchase requirement to be recorded and matched.",
                "request_evidence" to "Wants data samples, verified-company evidence, service report or success case.",
                "compare_packages" to "Asks about tiers, 3880/7880 entitlements, price, points or fees.",
                "resolve_objection" to "Challenges accuracy, effectiveness, service, platform value or contract terms.",
                "contract_or_payment" to "Ready to discuss contract, payment or approval process.",
                "after_sale" to "Existing member asks about delivery, remaining quota, balance or complaint.",
                "other" to "Unclear, casual or not classifiable."
            )
        ))
        put("she_needs", choice(
            "What SINGLE key field or resource is missing for the NEXT honest sales action? Choose none when adequate.",
            linkedMapOf(
                "main_product" to "What product(s) they produce, supply or seek.",
                "grade_spec" to "Procurement grade, brand, purity, packaging or use specification.",
                "quantity" to "Purchase quantity or monthly volume.",
                "destination" to "Destination / receiving district or delivery terms.",
                "purchase_time" to "Next buying date or quotation deadline.",
                "target_industry" to "Downstream application industry or buyer type.",
                "target_region" to "Area where they want customers or suppliers.",
                "factory_preference" to "Whether factories only, traders also or verification level needed.",
                "decision_maker" to "Whether the contact can approve the service and who handles contracts.",
                "verified_data" to "A real inquiry, supplier quote, company evidence, service report or up-to-date figures.",
                "contract_terms" to "Current approved contract, point deduction, fee, expiry or refund terms.",
                "none" to "Enough is known for the next limited action."
            )
        ))
        put("best_action", choice(
            "Select ONE next operational action. Avoid pressure, unverified facts or invented inquiries.",
            linkedMapOf(
                "clarify_business" to "Ask about business role, main products or raw-material sourcing.",
                "clarify_inquiry" to "Ask ONE missing procurement field: specs, quantity, receiving area or timing.",
                "clarify_targets" to "Ask product, target industry/region and factory-versus-trader preference.",
                "query_wenshu" to "Ask authorized 问数/data team for factual company, chain, price or supplier data.",
                "show_verified_sample" to "Offer an approved and privacy-safe sample with verification status.",
                "explain_manual_screening" to "Explain system matching plus human calling and evidence limitations.",
                "explain_package" to "Present documented 3880/7880 rights relevant to stated needs.",
                "explain_terms" to "Verify and explain actual fee, inquiry count, credits and contract/refund rules.",
                "record_inquiry" to "Save confirmed procurement details; submit only after required fields are checked.",
                "match_supplier" to "Look for eligible suppliers and authorized quotations.",
                "human_followup" to "Escalate technical, regulatory, complaint, pricing or contract exception.",
                "follow_up" to "Acknowledge, agree a follow-up or respect no-interest."
            )
        ))
        put("danger_level", score(
            "Rate RISK OF MAKING AN UNGROUNDED SALES PROMISE on this chat; not chemical hazard or client worth.",
            listOf(
                "A brief no-commitment response is supported by evidence.",
                "Only optional context missing; avoid invented specifics.",
                "Identity or current need remains uncertain.",
                "Material customer/target or inquiry requirement missing.",
                "Several requirements missing; clarify one at a time.",
                "Any claimed data count or factory identity lacks verified source.",
                "Specific inquiry or supplier price/status has no checked record.",
                "Quotas, fees, price or service-effect claims lack approved terms.",
                "Customer complaint or sensitive enterprise/contact data needs human review.",
                "Unverified guarantee, invented inquiry, regulated chemical or contractual commitment must be stopped."
            )
        ))
        put("should_reply_now", noul(
            "Can we GIVE the specifically requested factual answer from supplied VERIFIED context, not just acknowledge or ask a question?",
            "Verified data or approved package rules support the requested precise fact.",
            "Data, authorization or facts are missing; say you will verify or ask one question."
        ))
        put("tension_resolved", noul(
            "Is a paid package discussion appropriate NOW? It is not readiness to pay and never bypasses consent.",
            "Customer asked about memberships or clearly expressed a matching acquisition or inquiry-service need.",
            "Still an unqualified buyer/inquiry, explicit no-interest, unresolved complaint or no relevant service need."
        ))
        put("literal_question", noul(
            "Does this exchange need an authorized HUMAN to verify sensitive data, factory identity, compliance, actual inquiry, contract, refund or performance promise before assertion/action?",
            "A consequential or restricted fact requires authorized verification or manual service.",
            "No human-only assertion/action has been requested."
        ))
        put("inquiry_readiness", choice(
            "For a BUYER procurement inquiry, identify readiness to submit to the operating center. " +
                "Require product, spec/grade when applicable, quantity and receiving area; real buying intention is confirmed. " +
                "If not a buyer procurement conversation select not_applicable.",
            linkedMapOf(
                "complete" to "Necessary details and actual purchasing request are evidenced; human checks before release.",
                "needs_details" to "A real purchasing request exists but one or more key fields are missing.",
                "market_only" to "Only collecting market prices; no actionable purchase requirement confirmed.",
                "not_applicable" to "Not currently handling a buyer's procurement inquiry."
            )
        ))
    }

    /**
     * Build Jev state from a snapshot (last 10 messages), optionally carrying
     * the D-stage knowledge context.
     *
     * @param background relationship + contact notes + matched knowledge notes.
     * @param history older messages for this contact, already de-duplicated
     *        against what is on screen.
     *
     * Both extra fields are omitted when empty, so a user with no knowledge base
     * sends exactly the same body v1.2 did.
     */
    fun buildState(
        snapshot: ChatSnapshot,
        relationship: String,
        background: String = "",
        history: List<LogEntry> = emptyList()
    ): JSONObject {
        val msgs = JSONArray()
        val last10 = snapshot.messages.takeLast(10)
        for (m in last10) {
            msgs.put(JSONObject().put("from", m.side).put("text", m.text))
        }
        val chat = JSONObject()
            .put("relationship", relationship)
            .put("messages", msgs)
            .put("latest_from", last10.lastOrNull()?.side ?: "other")
        val state = JSONObject().put("chat", chat)
        if (background.isNotBlank()) state.put("background", background)
        if (history.isNotEmpty()) {
            val h = JSONArray()
            history.forEach { h.put(JSONObject().put("from", it.side).put("text", it.text)) }
            state.put("history", h)
        }
        return state
    }

    /** Jev compares candidate sales replies, preferring grounded and actionable text. */
    fun rankQuestion(candidates: List<String>): JSONObject {
        require(candidates.size == 3) { "rankQuestion expects exactly 3 candidates" }
        val keys = listOf("reply_a", "reply_b", "reply_c")
        val criteria = JSONObject()
        keys.forEachIndexed { i, k -> criteria.put(k, candidates[i]) }
        val q = JSONObject().apply {
            put("type", "choice")
            put("instructions",
                "Which Chinese reply is a safe and useful next step for 芯化和云 B2B business development? " +
                    "Honor the Jev next action: buyer inquiry discovery, supplier lead service, manual screening, " +
                    "approved package rights, objections or follow-up. Ask ONE missing field at a time. " +
                    "Never invent a purchase order, transaction, factory verification, MCP result, data count, " +
                    "success case or refund guarantee. Do not say 3888: approved basic price is 3880, " +
                    "professional is 7880. Package quotas are entitlements, NOT promised achieved orders. " +
                    "Avoid claiming 95% accuracy, 80% conversion or risk-free outcome without proof. " +
                    "Respect refusals; protect company and personal information." + BACKGROUND_NOTE)
            put("criteria", criteria)
        }
        return JSONObject().put("best_reply", q)
    }
}
