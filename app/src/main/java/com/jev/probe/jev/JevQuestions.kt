package com.jev.probe.jev

import com.jev.probe.core.ChatSnapshot
import com.jev.probe.core.kb.LogEntry
import org.json.JSONArray
import org.json.JSONObject

/**
 * Fixed Jev question set, ported verbatim from tools/jev/questions.py (the
 * wording that passed calibration). Instructions/criteria in English; chat text
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
     * Chemical B2B sales decisions. The answer keys intentionally retain the
     * original wire format so old callers can be migrated without changing the
     * Jev HTTP client. See [ChemicalDecisionLabels] for their business meanings:
     * true_intent = inquiry intent; danger_level = missing-information / commercial
     * risk; she_needs = missing requirement; should_reply_now = can give a specific
     * answer; best_action = next action; tension_resolved = quote readiness;
     * literal_question = needs human technical/compliance review.
     *
     * These are suggestions, NEVER approval to quote, promise stock, or certify a
     * substance. Real prices, documents and availability require verified data.
     */
    fun judge(): JSONObject = JSONObject().apply {
        put("literal_question", noul(
            "Does the customer's latest inquiry require qualified human review before a factual or technical answer? " +
                "Review includes hazardous chemicals, regulated sales/transport, product substitution, compatibility, " +
                "process safety, toxicology, technical certification or a request to guarantee a result. " +
                "Do not treat ordinary price/quantity questions as inherently hazardous.",
            "A regulatory, safety, substitution, certification, or technically consequential assertion needs verification.",
            "This is an ordinary sales inquiry that does not request a safety, regulatory, or technical conclusion."
        ))
        put("true_intent", choice(
            "Classify the CURRENT customer's commercial intent using explicit chat evidence. " +
                "Do not infer commitment or buying power from politeness. Prefer the latest actionable request.",
            linkedMapOf(
                "product_inquiry" to "Asking whether a specific chemical/product, brand, grade or alternative is offered.",
                "request_quote" to "Asking a price, quotation, rate or total cost for a product.",
                "negotiate_price" to "Discussing a discount, a competitor quote, price concessions or payment terms.",
                "request_sample" to "Asking for a sample, trial quantity or evaluation.",
                "request_documents" to "Requesting SDS, TDS, COA, certifications or technical parameters.",
                "delivery_order" to "Asking about stock, dispatch, lead time, delivery, existing order or logistics.",
                "after_sales" to "Reporting quality, shortage, returns, complaints or another post-sale issue.",
                "other" to "Unclear or non-commercial talk; none of the above is established."
            )
        ))
        put("danger_level", score(
            "How much missing information or commercial uncertainty would make an immediate specific " +
                "sales commitment risky? Use only the conversation and background facts. " +
                "This is NOT a chemical hazard classification; do not infer legal compliance from this score.",
            listOf(
                "Enough verified context for a safe, non-binding factual response.",
                "A minor optional detail is missing; respond without a firm commitment.",
                "A small clarification is needed, such as grade or packaging.",
                "One material quotation term such as quantity or destination is missing.",
                "Multiple material product or delivery details are unclear.",
                "Customer compares prices but equivalence of grade or delivery terms is unknown.",
                "Customer expects a price, inventory or delivery promise without verified records.",
                "A consequential product specification, certificate or contractual term is unverified.",
                "Regulated, hazardous or technically sensitive request needs qualified human review.",
                "High-consequence safety, compliance or technical claim must not be made from chat alone."
            )
        ))
        put("should_reply_now", noul(
            "Is there enough VERIFIED information in the supplied conversation/background to give the " +
                "customer a specific substantive answer to the latest request now, without inventing " +
                "price, inventory, COA, shipping promise or chemical performance? A clarification or " +
                "acknowledgment alone does not count as a specific substantive answer.",
            "The requested specific fact is supported by provided verified records and is safe to communicate.",
            "A material fact is missing or unverified: ask a focused question or say you will check."
        ))
        put("best_action", choice(
            "Choose the safest useful NEXT sales action for the latest message; never authorize a " +
                "transaction, regulated sale, final technical recommendation or confirmed quotation.",
            linkedMapOf(
                "clarify_product" to "Ask for exact chemical identity/CAS, grade, purity, application or packaging.",
                "clarify_quantity" to "Ask for purchase quantity or expected recurring volume.",
                "clarify_delivery" to "Ask for destination, needed date or delivery/incoterm requirements.",
                "check_price" to "Check a current authorized price and comparable quote conditions.",
                "check_stock" to "Check inventory or confirmed lead time in the business system.",
                "send_documents" to "Locate verified SDS/TDS/COA for the correct product and batch.",
                "arrange_sample" to "Confirm sample specifications and follow the approved sample process.",
                "human_review" to "Escalate to sales manager, compliance or qualified technical personnel.",
                "follow_up_order" to "Look up the existing order and confirm its status.",
                "acknowledge" to "Acknowledge the request or close the conversation without inventing facts."
            )
        ))
        put("she_needs", choice(
            "Which SINGLE piece of information is most urgently missing to handle this inquiry? " +
                "Select none only if the needed facts are in the supplied verified background.",
            linkedMapOf(
                "product_identity" to "Exact name, CAS, brand or unambiguous product identity is needed.",
                "grade_spec" to "Purity, grade, product specification, application or packaging is needed.",
                "quantity" to "Required quantity or unit is needed.",
                "destination" to "Destination, delivery condition or shipping address is needed.",
                "delivery_date" to "Required date or delivery window is needed.",
                "verified_price" to "A current authorized price and its validity/terms are needed.",
                "verified_stock" to "Actual inventory or lead-time verification is needed.",
                "documents" to "Relevant, verified SDS/TDS/COA or batch documents are needed.",
                "none" to "No key information is missing for a responsible next response."
            )
        ))
        put("tension_resolved", noul(
            "Is the inquiry READY for a specific quotation based on the supplied VERIFIED facts? " +
                "Require unambiguous product/grade, quantity, delivery terms and authorized current pricing. " +
                "Being friendly or showing buying intent is not enough.",
            "Essential terms and a current authorized price are verified in the context.",
            "Any essential term or authorized price is absent, uncertain or not verified."
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
                "Which Chinese chemical B2B sales reply is the most useful and factually grounded? " +
                    "Prefer a reply matching the customer's latest request and next sales action. " +
                    "Penalize invented prices, stock, lead time, purity, certifications, regulatory status, " +
                    "chemical compatibility, or guaranteed performance. Where facts are missing, prefer " +
                    "a focused clarification or an explicit offer to verify. Escalate safety and " +
                    "technical conclusions to qualified humans; never claim final approval." + BACKGROUND_NOTE)
            put("criteria", criteria)
        }
        return JSONObject().put("best_reply", q)
    }
}
