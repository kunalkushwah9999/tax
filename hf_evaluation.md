# Hugging Face evaluation plan

Research date: 31 October 2026. These are candidates, not a claim that they are the newest or best models. No model inference, local speed test or tax-domain benchmark was run. Apache-2.0 was displayed on each inspected publisher card; pin and review the exact redistributed artifact and dependencies before release.

## Candidates and priority

| Candidate | Role in this product | Limits and decision |
|---|---|---|
| [PaddlePaddle/en_PP-OCRv5_mobile_rec](https://huggingface.co/PaddlePaddle/en_PP-OCRv5_mobile_rec) | English text-line recognition in scanned tax documents | A recognizer requires detection, layout, preprocessing and field mapping. Try after native PDF extraction. No automatic confirmation. |
| [PaddlePaddle/devanagari_PP-OCRv5_mobile_rec](https://huggingface.co/PaddlePaddle/devanagari_PP-OCRv5_mobile_rec) | Devanagari text-line recognition | Evaluate Hindi numerals and labels separately. A generic English/Chinese recognizer does not establish Hindi coverage. |
| [Qwen/Qwen3-Embedding-0.6B](https://huggingface.co/Qwen/Qwen3-Embedding-0.6B) | Multilingual retrieval of relevant reviewed official source paragraphs | Filter by Act/year/status before retrieval. Vectors are similarity search, not authority or legal applicability. |
| [Qwen/Qwen3-Reranker-0.6B](https://huggingface.co/Qwen/Qwen3-Reranker-0.6B) | Rerank the small retrieved candidate set | Add only if measured retrieval improves enough to justify latency. Not a tax verifier. |
| [Qwen/Qwen3-4B-Instruct-2507](https://huggingface.co/Qwen/Qwen3-4B-Instruct-2507) | Optional strict-schema extraction or explanation draft | Accept only validated proposals. Explanation numbers/provisions must match supplied result evidence; template fallback. |

## Adoption order

1. No model: parse digital text/structured documents and render deterministic calculation explanations.
2. OCR benchmark: English recognition; add Devanagari only for actual document demand. Run in an isolated worker.
3. Reviewer retrieval: keyword baseline against reviewed sources; add embeddings and then reranking only when they improve retrieval on the evaluation set.
4. Optional language-model experiment for messy inputs. No general chatbot, DB tool, arbitrary URL tool or filing/payment tool is attached.

An “Indian tax” dataset or finance-trained model is not a legal source. Inspect license, document provenance, tax year, duplicate contamination and whether labels were written by another model. Do not train or evaluate “accuracy” using answers from the same assistant that invented the expected outputs. No such dataset is approved in this pack.

## Evaluation fixtures and proposed gates

These are proposed internal gates, not measured results or statistical guarantees.

| Track | Dataset and measurement | Gate before adoption |
|---|---|---|
| OCR/extraction | At least 200 permitted synthetic/deidentified documents across native PDF, scans, phone photos, multiple employers and layouts; a held-out issuer/layout set | Critical money/identity/date field exact-match, coverage/abstention and false auto-accept rates reported separately; every critical extracted fact still confirmed before use |
| Retrieval | At least 250 reviewer-labelled questions including wrong-year and similar-section distractors | Proposed Recall@10 >=95%; zero cross-tenant retrieval; explicit wrong-year exclusions; measure reranker lift |
| Explanation | At least 100 structured-result fixtures plus malicious document text | All emitted amounts, year labels and legal references match allowed result/evidence fields; invalid output falls back to templates |
| Operations | Fixed input lengths and concurrency on declared CPU/RAM/GPU/runtime | p50/p95/p99 latency, peak RAM, cost per accepted document and timeout/abstention recorded; reject a model that fails the queue budget |
| Privacy/security | Prompt injection, hostile file, unauthorized field and cross-tenant fixtures | No tool/data exfiltration path, no persistent document prompts, no unapproved source/fact writes; independent review of the actual runtime |

Do not report a published benchmark score as accuracy on Form 16 or Indian tax law. Quantization, tokenizer/runtime and revision changes require relevant regression and performance checks. A sample with no observed errors is not proof of zero future error.

## Source and retrieval contract

Each source chunk has official URL, source hash, provision, Act, income period, publication date, effective interval, jurisdiction where relevant, review status and reviewer decision. Filter these structured dimensions before semantic ranking. Retain the retrieved chunk IDs and index revision for explanation provenance. If source versions conflict, return a review task rather than blend them.

Do not embed customer financial documents into a shared public/source index. If private retrieval is eventually needed, isolate by tenant/profile/consent and remove vectors with the associated retention/deletion process. The final tax engine reads its signed rule bundle; it never reads a vector search result as executable law.

## Model supply chain and serving

- Download only reviewed publisher artifacts; record immutable revision, weight/tokenizer/config digests, license notices and runtime dependency lock.
- Prefer safetensors and audited conversion paths. Keep trust_remote_code false by default; a security scanner is not a safety guarantee. Review any exception offline. [H06,H07]
- Test with fake financial documents, not real PANs in public Spaces, widgets or demos. Public demos are not a production privacy contract.
- Inference workers have no direct tax-ledger write, KMS key, admin token or Internet browsing capability. Validate output against an allowlisted schema; unknown remains unknown.
- Route through one model gateway with time/size/token/cost limits, cancellation, tenant isolation and version logging without raw document content.
- Any hosted provider needs data-processing, retention/training, access and location review. Do not infer privacy terms from a model's open license.
- Keep a deterministic/manual fallback for a model outage. A changed provider/model never silently changes approved rules or facts.

## Laptop and cost

On a 16 GB U-series laptop, start with small OCR fixtures or embedding batches and measure actual resource use. The 0.6B model size is not an end-to-end RAM estimate. A 4-bit 4B model may be an experiment, but runtime overhead, context/KV cache and other apps can make it impractical. No responsiveness promise is made without a measurement. Production concurrency needs a separate measured worker budget.

Model downloads and inference can be free of license fees while still requiring paid compute, storage and operations. Avoid building around a free hosted quota for a deadline-sensitive tax product.

Machine-readable candidate decisions are in hf_candidates.json. All production_approved fields are false and all unmeasured metrics/revisions are null.
