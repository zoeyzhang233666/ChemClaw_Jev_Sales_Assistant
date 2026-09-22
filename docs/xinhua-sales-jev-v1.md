# ChemClaw-Jev 销售助手 V1（第一轮代码）

## 范围

本分支在 Android 原聊天采集/悬浮窗上改造 Jev，服务芯化和云**采购商开发**与**供应商商机服务销售**，不是将原项目改为化学品零售报价工具。微信/QQ/X/飞书沿用原有适配器，企业微信适配**尚未实现**；电话沟通记录也还没有输入界面。所有候选消息必须业务员自行核对和发送。

销售流程：聊天采集 → Jev 判断客户角色、企业自述性质、阶段、服务方向、异议、缺失信息、询盘状态、下一步动作 → 用确认过的套餐资料和知识库起草 3 条话术 → Jev 带上第一轮决策再判断 3 条回复 → 人工发送。

## Jev 输出

| 输出 | 可观察内容 |
| --- | --- |
| customer_role | buyer/supplier/both/unknown：角色可双向 |
| company_type | claimed_factory/claimed_trader/mixed/unknown：**不等于核验** |
| sales_stage | prospecting/discovery/trust_building/package_discussion/objection/contract_payment/delivery_followup/unknown |
| service_direction | find_downstream/find_supplier/verify_factory/inquiry_matching/transaction_match/package_rights/other |
| objection | data_accuracy/price/effectiveness/trust/duplicates/platform/rights_refund/no_need/other/none |
| true_intent | 客户本轮沟通意图 |
| she_needs | 只选择下一个最重要的缺失字段 |
| best_action | 只选下一步业务动作，query_wenshu 为**待执行的查询建议** |
| inquiry_readiness | complete/needs_details/market_only/not_applicable |
| danger_level | 销售承诺及信息不足风险，**不是化学危险分级** |
| should_reply_now | 是否有事实依据具体答复 |
| tension_resolved | 是否适合讨论会员套餐，不代表购买承诺 |
| literal_question | 是否需要人工核验/审核 |

旧字段名保持兼容，避免原 UI/Java 客户端大量破坏；业务含义已改变。

## 套餐：以用户提供的权益图为准

| 权益 | 每日询单企业基础版 | 每日询单企业专业版 |
| --- | ---: | ---: |
| 现价 | ¥3880 | ¥7880 |
| 有效期 | 12个月 | 12个月 |
| 意向询单额度 | 45条 | 90条 |
| 主打产品 | 10个 | 30个 |
| 订阅产品 | 50个 | 200个 |
| 专家撮合服务 | 10 | 25 |
| 企业认证专属标签 | 无 | 有 |
| 行业顾问咨询、积分兑现 | 有 | 有 |
| 供应链金融 | 无 | 有 |

45/90 为用户确认的**一年可获取的经过筛选的意向询单额度**；不是保证成交 45/90 单。基础价为 3880 元，不是早期口头提到的 3888 元。套餐金额、合同与积分退款规则不得自行推导；具体扣款、超额交付、退款、到期操作等须核实当前合同。

## 问数/MCP 集成边界

公司已上线「问数」（千万化工数据，一句话查询）及数据 MCP。此轮**没有**在 Android 填入员工或全公司 API Key，也**没有**声称已经调用 54 个工具或连通问数。Jev 的 query_wenshu 表示建议业务员去查，不表示查询成功。接入前需要确认内部问数 Agent 的服务端调用协议、权限校验、工具响应字段和 L1–L4 数据权限；推荐服务端按业务员身份授权、审计、最小化返回，不将完整数据集下发到手机。内容运营 Agent 可后续用于经审核的客户沟通素材，不在本轮范围。

## 业务真实性及保护

- 间接询单开场仅适用于真实、获得授权的采购任务；不得假装虚假买家。
- 系统与 AI 推断的工厂身份和人工电话确认的工厂身份必须区分。
- 未证实的数量、成交案例、价格、询盘或竞品比较不作为承诺。
- 不把套餐权益和实际结果混为一谈；不把“可申请退款”说成“无条件退全款”。
- 企业客户、电话/微信联系方式和已成交信息要受既有 L1–L4 权限控制。
- 涉及危化品、资质和技术问题由人核验；Jev 不替代合规决策。

## 手工验收场景（应在真实 Jev API 上做标注测试）

| 输入客户最新消息 | 预期主要判断 |
| --- | --- |
| 我们做钛白粉，想找广东真正生产涂料的厂家 | supplier / find_downstream / clarify_targets 或 query_wenshu |
| 我们片碱每月用10吨，送江西，先了解行情 | buyer / find_supplier；明确只看行情时 market_only |
| 我买甲醇，99%，20吨，送江西南昌，下周采购 | buyer / inquiry_readiness=complete（发布前仍核验） |
| 别家给的全是贸易商，你们怎么验证工厂 | objection=data_accuracy / explain_manual_screening |
| 基础版和专业版什么区别 | compare_packages / explain_package；3880/7880 |
| 交钱后服务差了能退吗 | rights_refund / explain_terms，核对现行合同 |
| 发给我一个你们已经成交的询盘 | request_evidence，未查到证据不能编 |
| 不考虑会员，只想看市场行情 | no_need；不催购买、不制造虚假采购意图 |

## 验证状态

已提交 GitHub Kotlin 代码并开 Draft PR，**并不等于已经通过 Android 编译、真机测试或 Jev 在线精度测试**。无公司 API Key、真实 CRM/MCP API 文档、授权和脱敏样本前，不做数据接口实接。建议先在 GitHub Actions 或有 Android SDK 的电脑上运行 `./gradlew assembleDebug`；再以 20–50 条脱敏真实采购商/供应商对话建立有人工标注的 Jev 测试集。
