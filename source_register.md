# Source register

Checked on 2026-10-31 (India time). Web review and engineering recommendations are not CA sign-off.

| ID | Primary source | What it supports / access limit |
|---|---|---|
| T01 | [Income Tax Department: AY 2026-27 ITR-2 FAQ](https://www.incometax.gov.in/iec/foportal/help/all-topics/e-filing-services/itr-2/itr-2-faqs) | New-regime slabs and rebate. Guidance, not a replacement for legislation. |
| T02 | [Finance Act amendment to section 87A](https://wmstatic-prd.incometaxindia.gov.in/web/guest/w/section-20-111) | Finance Act amendment: increased threshold/rebate; special-rate restriction. |
| T03 | [Income Tax Department: Budget 2026 FAQ](https://incometaxindia.gov.in/Documents/Budget2026/FAQs-Budget-2026.pdf) | Official indexed FAQ describes relief and the approximate end of its zone. Direct PDF fetch failed in this run; indexed official text was available. |
| T04 | [CBDT Circular 28/2016, 27 July 2016](https://incometaxindia.gov.in/communications/circular/cirularno282016.pdf) | 1 April birthdays and age attained on preceding 31 March. Indexed official text reviewed; direct PDF returned 403. |
| T05 | [Income-tax Act 1961: section 288A](https://www.incometaxindia.gov.in/w/section-288a-62) | Total-income rounding. Ignore paise, then nearest multiple of ten. |
| T06 | [Income-tax Act 1961: section 288B](https://www.incometaxindia.gov.in/w/section-288b-58) | Amount payable/refund rounding. Final settlement order must be matched to the utility. |
| T07 | [Income-tax Act 1961: section 80TTA](https://www.incometaxindia.gov.in/w/section-80tta-12) | Eligible savings interest; excludes time deposits; aggregate limit. |
| T08 | [Income-tax Act 1961: section 80TTB](https://www.incometaxindia.gov.in/w/section-80ttb-10) | Resident senior citizen and qualifying deposits; aggregate limit. |
| T09 | [Income Tax Department: section 16 statutory text](https://wmstatic-prd.incometaxindia.gov.in/documents/20117/42998/Section-16_2025-11-01_12-37-38_06ad41_en.pdf/3305b6d9-5c8c-3356-17af-be487622fa6c?t=1774544869814&version=1.0) | Salary standard deduction and salary cap. |
| T10 | [Notification 45/2026, GSR 226(E), 30 March 2026](https://www.incometax.gov.in/iec/foportal/sites/default/files/2026-04/Notification%20No.45_2026.pdf) | Notified AY 2026-27 ITR-1 and two house properties. Prefer this over the stale one-property summary. |
| T11 | [Income Tax Department: ITR-1 FAQ](https://www.incometax.gov.in/iec/foportal/help/all-topics/e-filing-services/ITR1-FAQ) | Updated ITR-1 eligibility and exclusions. Form scope is broader than the proposed starter calculator. |
| T12 | [Income Tax Department: return downloads](https://www.incometax.gov.in/iec/foportal/downloads/income-tax-returns?mobile-app=1) | AY 2026-27 utility and schema versions. Record the exact artifact used for each differential test. |
| T13 | [Income Tax Department: new Act return FAQ](https://www.incometax.gov.in/iec/foportal/node/11724) | FY 2025-26 / AY 2026-27 remains under 1961 Act; TY 2026-27 is a separate obligation. |
| T14 | [Income Tax Department: objective/scope of new Act](https://www.incometax.gov.in/iec/foportal/help/all-topics/e-filing-services/objective-and-scope-new-act-faq) | Transition and new tax-year terminology. Do not relabel the old rules as the new pack. |
| T15 | [Income Tax Department: deductions and rebates](https://wm.incometaxindia.gov.in/w/deductions-allowable-to-tax-payer) | Old-regime 5 lakh threshold and 12,500 rebate; corroborates the separate old/new rule. |
| T16 | [Income Tax Department: Chapter VI-A regime restrictions](https://wmstatic-prd.incometaxindia.gov.in/web/guest/w/deductions) | Section 115BAC restricts Chapter VI-A claims; 80TTA/80TTB are not among the listed permitted exceptions. |
| L01 | [MeitY: DPDP Rules 2025, GSR 846(E)](https://www.meity.gov.in/static/uploads/2025/11/53450e6e5dc0bfa85ebd78686cadad39.pdf) | Staged commencement; security, retention and breach provisions. Legal mapping is still a release task. |
| L02 | [MeitY: DPDP Rules corrigendum, GSR 892(E)](https://www.meity.gov.in/static/uploads/2025/12/3c7ebbae0e5456f493f486e6845df86b.pdf) | Apply corrigendum when reviewing the Rules. |
| L03 | [CERT-In Directions, 28 April 2022](https://www.cert-in.org.in/PDF/CERT-In_Directions_70B_28.04.2022.pdf) | Covered incident reporting and ICT-log requirements; applicability must be mapped to the business. |
| L04 | [SEBI: Investment Advisers Regulations, consolidated listing](https://www.sebi.gov.in/legal/regulations/nov-2025/securities-and-exchange-board-of-india-investment-advisers-regulations-2013-and-securities-last-amended-on-november-25-2025-_98246.html) | Latest consolidated regulation located in this review. Personalised category allocation is not automatically outside regulation. |
| L05 | [SEBI: Master Circular for Investment Advisers, February 2026](https://www.sebi.gov.in/sebi_data/attachdocs/feb-2026/1770375291405.pdf) | Current compliance material for a qualified legal review, not automatic permission for this platform. |
| L06 | [PIB: May 2026 cybersecurity workshop](https://www.pib.gov.in/PressReleasePage.aspx?PRID=2261823&lang=1&reg=3) | Government statement identifies May 2027 full DPDP enforceability. Build early; do not hardcode an unreviewed legal deadline. |
| H01 | [Qwen3 Embedding 0.6B, publisher model card](https://huggingface.co/Qwen/Qwen3-Embedding-0.6B) | Apache-2.0 on inspected card; retrieval candidate. |
| H02 | [Qwen3 Reranker 0.6B, publisher model card](https://huggingface.co/Qwen/Qwen3-Reranker-0.6B) | Apache-2.0 on inspected card; ranking candidate. |
| H03 | [PaddleOCR English recognizer, publisher model card](https://huggingface.co/PaddlePaddle/en_PP-OCRv5_mobile_rec) | Apache-2.0 on inspected card; recognises text lines, not a complete document pipeline. |
| H04 | [PaddleOCR Devanagari recognizer, publisher model card](https://huggingface.co/PaddlePaddle/devanagari_PP-OCRv5_mobile_rec) | Apache-2.0 on inspected card; language-specific OCR candidate. |
| H05 | [Qwen3 4B Instruct 2507, publisher model card](https://huggingface.co/Qwen/Qwen3-4B-Instruct-2507) | Apache-2.0 on inspected card; optional constrained extraction/explanation baseline. |
| H06 | [Hugging Face Hub: pickle security](https://huggingface.co/docs/hub/security-pickle) | Model serialization security. Prefer reviewed safe formats; scans are not a guarantee. |
| H07 | [Hugging Face Transformers: custom models](https://huggingface.co/docs/transformers/custom_models) | Review custom code before execution. Keep remote code disabled by default. |

## Evidence limits
- No official utility was operated and no CA reviewed or signed the golden cases in this run.
- No model weights were downloaded or executed. Publisher cards establish candidate capabilities and license declarations, not Indian tax accuracy.
- No original runnable project, Maven configuration, migrations or deployment was provided. Source snippets are incomplete evidence of an application.
- Government summary pages can lag notifications. Record conflicts and their resolution instead of silently choosing a convenient source.
- Links can change. At implementation, retain the permitted official source bytes, content digest, publication date, provision, applicability and review decision.
- The old-law section 16 source carries an amendment proviso. The approved bundle must use consolidated applicable law, not mechanically parse amendment language as a separate deduction.
- Exact DPDP commencement/application should be entered in the legal register after reviewing the notifications and corrigendum. Do not confuse notification date, upload date and a date cited by a secondary article.

## Revision 1 sources

| ID | Primary source | Use |
|---|---|---|
| T17 | [Income Tax Department: old/new regime FAQ](https://www.incometax.gov.in/iec/foportal/help/new-tax-vs-old-tax-regime-faqs?mobile-app=1) | Regime choice depends on filing/business context; actual legal fixtures still require approval. |
| S01 | [OWASP Authorization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html) | Authorization at every request and protection of static/export resources. |
| S02 | [Google SRE Workbook: Canarying Releases](https://sre.google/workbook/canarying-releases/) | Candidate/control rollout and version-attributed evaluation; tax-specific controls are proposed here. |

Checked 31 October 2026. These references do not certify this implementation or provide approved numerical golden values.

## Revision 2 implementation references

| ID | Primary source | Use |
|---|---|---|
| J01 | [Java 17 sealed classes](https://docs.oracle.com/en/java/javase/17/language/sealed-classes-and-interfaces.html) | Restricted public-result and fact hierarchies. |
| J02 | [Java 17 BigDecimal](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/math/BigDecimal.html) | Exact decimal transport normalization/comparison; no statutory tax rounding implemented in this component. |

S01 remains the primary authorization reference. Java/runtime/test results are
observations from this workspace, not claims established by documentation links.

## Revision 3 implementation references

| ID | Primary source | Use |
|---|---|---|
| J03 | [Java 17 Signature API](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/security/Signature.html) | JCA signature verification |
| J04 | [Java 17 providers](https://docs.oracle.com/en/java/javase/17/security/oracle-providers.html) | Ed25519 provider support; no hardcoded provider name |
| S03 | [OWASP Input Validation](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html) | Bounded allowlisted input syntax and semantics |
| S04 | [OWASP Key Management](https://cheatsheetseries.owasp.org/cheatsheets/Key_Management_Cheat_Sheet.html) | Key trust, separation and lifecycle boundaries |

These documentation sources do not establish the observed pass counts, production
deployment, tax correctness or security of unsupplied key/control adapters.

## Revision 5 implementation references

| ID | Primary source | Use |
|---|---|---|
| J05 | [Java 17 ConcurrentHashMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html) | Concurrent session lookup; session monitors provide compound edit publication |
| J06 | [Java 17 Clock](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Clock.html) | Injected trusted server time and deterministic expiry tests |

S01 remains the authorization reference. Documentation does not establish SQL
isolation, durable storage, tax correctness or the observed pass counts.

## Revision 6 official professional workflow references

| ID | Primary source | Scope |
|---|---|---|
| T18 | [Form 3CA-3CD manual](https://www.incometax.gov.in/iec/foportal/help/statutory-forms/popular-form/form3ca-3cd-um) | Applicable workflow has assigned registered CA and DSC; not automated audit applicability or a new-law pack |
| T19 | [My CA manual](https://www.incometax.gov.in/iec/foportal/help/how-to-work-with-ca) | Assignment/authorization validity, withdrawal and activation; client authorization is distinct from credential standing |
| T20 | [ERI add-client manual](https://www.incometax.gov.in/iec/foportal/help/addclient) | Registration and taxpayer-consent prerequisites; not evidence this platform has ERI/provider authorization |

Checked 31 October 2026. These sources do not establish tax accuracy, professional-
need rates, this platform's authorization or real credentials in synthetic tests.

## Revision 8 arithmetic-source recheck

On 1 November 2026, rechecked the official AY 2026-27 salaried page (T01/T15 scope),
288A/288B (T05/T06), 80TTA/80TTB (T07/T08), the 87A amendment (T02) and CBDT Circular
28/2016 (T04). The salaried page supplied old/new slabs, deduction caps and scope
context; it is guidance and includes stale form-summary material, so it is not
used to broaden starter/form support. The age circular PDF was retrieved.
Some statutory pages were available in official search text while direct fetch
returned 403/redirect failures. See the draft manifest for exact source URLs.

This supports a new draft engineering implementation and unit specifications.
No official utility was operated, no qualified reviewer signed numerical outputs,
no evidence bytes or real credentials were validated and no production rule was
published. New-Act law, broader schedules and the earlier legal/HF reviews were
not silently upgraded by this scoped recheck.

## Revision 9 statement-comparison source

T21: [Income Tax Department AIS FAQ](https://www.incometax.gov.in/iec/foportal/help/all-topics/e-filing-services/ais%20-%20annual%20information%20statement-faqs),
checked 1 November 2026. It distinguishes reported/processed/accepted values,
AIS/TIS and TDS/TCS information in 26AS. This informs keeping observations and
source declarations distinct, not the correctness of a native parser or a credit.
No native portal/statement was accessed and no official feedback was submitted.
The new schema is explicitly our bounded normalized sample contract.
