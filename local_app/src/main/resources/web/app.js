'use strict';
const definitions=[
  {field:'SALARY',label:'Annual salary',help:'Gross salary before standard deduction; combine employers. Keep interest separate.',sample:'8 lakh'},
  {field:'SAVINGS_INTEREST',label:'Savings account interest',help:'Keep savings interest separate from deposits.',sample:'5000 rupees'},
  {field:'DEPOSIT_INTEREST',label:'Qualifying deposit interest',help:'Qualifying bank, co-operative bank or post-office deposits; eligibility needs evidence.',sample:'0 rupees'},
  {field:'OTHER_INTEREST',label:'Other interest',help:'Gross interest before TDS, outside the qualifying categories above.',sample:'0 rupees'}
];
const inventoryDefinitions=[
  {field:'INCOME',label:'Income review',help:'Confirm the sample amounts before completing this review.'},
  {field:'NEW_DEDUCTIONS',label:'New regime checkpoint',help:'Records a sample review step, not a deduction calculation.'},
  {field:'OLD_DEDUCTIONS',label:'Old regime checkpoint',help:'Records a sample review step, not a definitive regime choice.'}
];
const titles={reconciliation:['Compare statements','SOURCE OBSERVATIONS · LOCAL REVIEW','See the differences.','Compare declared source observations without double-counting them.'],analysis:['Calculation check','STEP 03 · SCENARIO CHECK','Check this scenario.','Confirm the profile and screening for your prepared income inputs.'],prepare:['Prepare inputs','STEP 01 · INPUT PREPARATION','Prepare with clarity.','Enter an amount, check the proposal, then confirm it.'],review:['Review checkpoints','STEP 02 · EXPLICIT REVIEW','Review before moving ahead.','Each checkpoint follows the current confirmed sample inputs.'],validation:['Validation status','STEP 03 · RESULT VALIDATION','Know what is ready.','Preparation progress never unlocks an unvalidated tax result.'],history:['Change history','STEP 04 · SESSION HISTORY','See what changed.','Accepted edits and reviews, without monetary values in the history.']};
const errors={STALE_INPUT:'This draft changed in another tab. Review the current values before submitting again.',IDEMPOTENCY_CONFLICT:'That request ID belongs to a different edit. Refresh the draft before trying again.',CONFIRMATION_VALUE_CHANGED:'The amount no longer matches the proposal. Review the current proposal.',REVIEW_STATE_CHANGED:'That value is no longer awaiting this action. Refresh the current draft.',INPUT_REVIEW_INCOMPLETE:'Confirm all income amounts and complete the income review first.',INVALID_AMOUNT:'Check the number and unit. Nothing was changed.',AMOUNT_TOO_LONG:'Keep the sample amount under 65 characters.',SESSION_LIMIT_REACHED:'This sample reached its edit limit. Start a fresh sample session.',CSRF_REJECTED:'This page is out of date. Refresh it before making a change.',ORIGIN_REQUIRED:'Open the workspace from its local address.',TRY_AGAIN_LATER:'Please wait a moment before making more requests.',SESSION_REQUIRED:'Your sample session is unavailable. Start a fresh session.',SESSION_ENDED:'Your sample session ended. Start a fresh session.',INVALID_REQUEST:'This request could not be accepted.',UNEXPECTED_FIELDS:'This action contains unsupported fields.',TEMPORARILY_UNAVAILABLE:'The local service is temporarily unavailable. Try again.'};
Object.assign(errors,{SOURCE_SELECTION_REQUIRED:'Record the exact source observation before proposing it.',WHOLE_CATEGORY_REVIEW_REQUIRED:'Check that this is the whole annual category, including its classification.',REPLACEMENT_REVIEW_REQUIRED:'Acknowledge replacement of the existing input.',SOURCE_FIELD_MISMATCH:'The destination does not match the source category.',SOURCE_AMOUNT_CHANGED:'The displayed gross amount does not match the selected source.',CATEGORY_COVERAGE_AMBIGUOUS:'More than one group covers this category. Check and prepare an annual total separately.',GROSS_SOURCE_REVIEW_REQUIRED:'Resolve missing, conflicting or repeated gross observations before using a source proposal.',ANNUAL_SOURCE_REQUIRED:'A transaction cannot replace a whole annual input.',STALE_RECONCILIATION:'The source review belongs to an earlier draft or comparison. Compare and review again.',CATEGORY_NOT_PLANNER_INPUT:'This category needs a broader workflow.',COMPARISON_LIMIT_REACHED:'This session reached its local comparison limit. Start a fresh sample.'});
const inputErrors={MISSING:'Enter an amount, or use an explicit zero.',UNIT_REQUIRED:'Write a unit, such as “1 lakh”, or choose a unit beside the input.',AMBIGUOUS_UNIT:'The written amount and selected unit disagree.',INVALID_GROUPING:'Check comma grouping in the amount.',EXCESS_PRECISION:'Use an amount that converts exactly to paise.',MIXED_DIGIT_SCRIPTS:'Use one digit script within the amount.',OUT_OF_RANGE:'The amount is outside the supported transport range.',INVALID_NUMBER:'Enter a number without signs, formulas or exponent notation.',UNSUPPORTED_CURRENCY:'This sample accepts INR amounts.'};
const $=id=>document.getElementById(id);
const ui={state:null,active:'prepare',busy:false,filling:false,pending:null,reconPending:null,comparison:null,comparisonDirty:false,dirty:new Set()};
const controls=new Map();
function element(tag,className,text){const node=document.createElement(tag);if(className)node.className=className;if(text!==undefined)node.textContent=text;return node;}
function money(text){const [whole,fraction='00']=text.split('.');return '₹'+BigInt(whole).toLocaleString('en-IN')+'.'+fraction.padEnd(2,'0');}
function notice(text,isError=false){$('notice').textContent=text;$('notice').classList.toggle('error',isError);$('notice').hidden=!text;}
function statusLabel(state){return {UNKNOWN:'Missing',ESTIMATED:'Needs confirmation',CONFIRMED:'Confirmed',CONFLICT:'Resolve conflict'}[state]||state;}
function blocked(){return ui.busy||ui.filling||Boolean(ui.pending)||Boolean(ui.reconPending)||!ui.state;}

async function request(path,options={}){
  const response=await fetch(path,{credentials:'same-origin',cache:'no-store',...options});
  const data=await response.json();
  if(!response.ok){const error=new Error(inputErrors[data.input_problem]||errors[data.status]||'The request was not accepted.');error.responseKnown=true;error.data=data;error.httpStatus=response.status;throw error;}
  return data;
}
async function fetchState(){const previous=ui.state?.view_token;ui.state=await request('/api/state');if(previous!==ui.state.view_token){clearAnalysis();clearComparison(true);}render();}
async function start(){
  if(!ui.state){$('analysis-form').reset();$('comparison-csv').value='';clearComparison(false);}clearAnalysis();ui.busy=true;renderButtons();
  try{ui.state=await request('/api/session');ui.pending=null;$('session-ended').hidden=true;$('workspace-content').hidden=false;render();notice('Sample session ready. Your inputs are not saved for later.');}
  catch(error){ui.state=null;$('session-ended').hidden=false;$('workspace-content').hidden=true;notice(error.message,true);}
  finally{ui.busy=false;renderButtons();}
}
async function submit(fields,retryPayload=null){
  if(!ui.state)return false;
  const payload=retryPayload||{request_id:crypto.randomUUID(),view_token:ui.state.view_token,...fields};
  ui.busy=true;renderButtons();notice('');
  try{
    const receipt=await request('/api/edit',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8','X-CSRF-Token':ui.state.csrf_token},body:new URLSearchParams(payload).toString()});
    ui.pending=null;
    if(payload.field){ui.dirty.delete(payload.field);if(payload.operation==='CLEAR'){const control=controls.get(payload.field);control.input.value='';control.unit.value='';}}
    await fetchState();
    notice(receipt.status==='REPLAYED'?'The earlier request was already accepted. The current draft has been refreshed.':receipt.status==='NO_CHANGE'?'This review was already complete. No input change was needed.':'Updated. Review checkpoints reset when a material amount changes.');
    if(ui.active==='history')await history();return true;
  }catch(error){
    if(error.responseKnown){ui.pending=null;notice(error.message,true);if(error.httpStatus===401){ui.state=null;$('session-ended').hidden=false;$('workspace-content').hidden=true;}
      else if(error.httpStatus===409){try{await fetchState();}catch(refreshError){notice(refreshError.message,true);}}
    }else{ui.pending=payload;notice('The outcome is not confirmed. Retry the same request; do not submit it as a new edit.',true);}
    return false;
  }finally{ui.busy=false;renderButtons();}
}

function buildInputs(){
  for(const definition of definitions){
    const card=element('article','input-card');card.dataset.field=definition.field;
    const head=element('div','input-card-head');const heading=element('h3','',definition.label);const badge=element('span','badge','Missing');head.append(heading,badge);
    const form=document.createElement('form');form.autocomplete='off';
    const label=element('label','', 'Amount and unit');label.htmlFor='amount-'+definition.field;
    const row=element('div','amount-row');const input=document.createElement('input');input.id=label.htmlFor;input.type='text';input.placeholder='e.g. '+definition.sample;input.maxLength=64;input.spellcheck=false;input.setAttribute('aria-describedby','help-'+definition.field);
    const unit=document.createElement('select');unit.setAttribute('aria-label',definition.label+' unit');
    for(const [value,text] of [['','As written'],['RUPEES','Rupees'],['THOUSAND','Thousand'],['LAKH','Lakh'],['CRORE','Crore']]){const option=element('option','',text);option.value=value;unit.append(option);}
    row.append(input,unit);const help=element('p','field-help',definition.help+' Use a written unit or select one.');help.id='help-'+definition.field;
    const proposal=element('div','proposal');const proposalTitle=element('span','proposal-title','CURRENT VALUE');const amount=element('strong','empty-proposal','No amount entered');proposal.append(proposalTitle,amount);
    const actions=element('div','field-actions');const save=element('button','save-proposal','Save proposal');save.type='submit';const confirm=element('button','confirm-proposal','Confirm amount');confirm.type='button';const clear=element('button','clear-amount','Clear');clear.type='button';actions.append(save,confirm,clear);
    const dirty=element('p','dirty-note','Save the edited proposal before confirming.');dirty.hidden=true;
    const sourceLink=element('p','source-link');sourceLink.hidden=true;
    form.append(label,row,help,proposal,sourceLink,actions,dirty);card.append(head,form);$('input-cards').append(card);
    const change=()=>{ui.dirty.add(definition.field);renderButtons();};input.addEventListener('input',change);unit.addEventListener('change',change);
    form.addEventListener('submit',event=>{event.preventDefault();if(blocked())return;submit({operation:'PROPOSE',field:definition.field,text:input.value,unit:unit.value});});
    confirm.addEventListener('click',()=>{const current=ui.state?.inputs.find(value=>value.field===definition.field);if(current?.state==='ESTIMATED'&&!ui.dirty.has(definition.field)&&!blocked())submit({operation:'CONFIRM',field:definition.field,amount:current.amount});});
    clear.addEventListener('click',()=>{if(!blocked())submit({operation:'CLEAR',field:definition.field});});
    controls.set(definition.field,{input,unit,badge,proposalTitle,amount,save,confirm,clear,dirty,sourceLink});
  }
}
function buildReviews(){
  for(const definition of inventoryDefinitions){
    const card=element('article','checkpoint');const copy=element('div');copy.append(element('h3','',definition.label),element('p','',definition.help));
    const state=element('div','reviewed-label','Not reviewed');copy.append(state);const button=element('button','button small','Mark reviewed');button.type='button';
    button.addEventListener('click',()=>{if(!blocked())submit({operation:ui.state.reviews[definition.field]?'UNREVIEW':'REVIEW',inventory:definition.field});});
    card.append(copy,button);$('review-cards').append(card);controls.set(definition.field,{button,state});
  }
}
function render(){
  if(!ui.state){renderButtons();return;}
  for(const current of ui.state.inputs){const control=controls.get(current.field);control.badge.textContent=statusLabel(current.state);control.badge.className='badge '+(current.state==='ESTIMATED'?'estimated':current.state==='CONFIRMED'?'confirmed':'');
    control.proposalTitle.textContent=current.state==='CONFIRMED'?'CONFIRMED INPUT':current.state==='ESTIMATED'?'PROPOSED AMOUNT':'CURRENT VALUE';control.amount.textContent=current.amount?money(current.amount):'No amount entered';control.amount.classList.toggle('empty-proposal',!current.amount);
    const link=(ui.state.source_links||[]).find(row=>row.field===current.field);control.sourceLink.hidden=!link;control.sourceLink.textContent=link?'Historical unverified source link: '+link.source+' / '+link.value_kind+' · '+link.observation_id+' · '+link.period+'. Source authenticity is unverified; current income confirmation is separate.':'';
  }
  const guide=ui.state.intake.guide;const steps=guide.checklist||[];const reviewed=steps.filter(step=>step.status==='REVIEWED').length;
  $('progress-label').textContent=guide.progress_label||'Input review unavailable';$('progress-number').textContent=reviewed+'/8';$('preparation-progress').value=reviewed;
  $('checklist').replaceChildren(...steps.map(step=>{const li=element('li',step.status==='REVIEWED'?'complete':'');li.append(element('span','step-dot',step.status==='REVIEWED'?'✓':''),element('span','',step.id==='SUPPORTED_SCOPE'?'Sample scenario scope':step.label),element('span','check-status',({REVIEWED:'Reviewed',MISSING:'Missing',NEEDS_REVIEW:'Review needed',NEEDS_CONFIRMATION:'Confirm value',CONFLICT:'Resolve conflict'})[step.status]||step.status));return li;}));
  $('next-label').textContent=guide.next_action?.label||'Refresh your sample';$('next-help').textContent=guide.next_action?.help||guide.message;$('preparation-note-text').textContent=guide.preparation_note||'';
  for(const definition of inventoryDefinitions){const control=controls.get(definition.field);control.state.textContent=ui.state.reviews[definition.field]?'Reviewed for this sample':'Not reviewed';control.button.textContent=ui.state.reviews[definition.field]?'Reopen review':'Mark reviewed';}
  renderButtons();
}
function renderButtons(){
  const locked=blocked();
  for(const definition of definitions){const control=controls.get(definition.field);if(!control)continue;const current=ui.state?.inputs.find(value=>value.field===definition.field);
    control.input.disabled=locked;control.unit.disabled=locked;control.save.disabled=locked;control.confirm.disabled=locked||current?.state!=='ESTIMATED'||ui.dirty.has(definition.field);control.clear.disabled=locked||current?.state==='UNKNOWN';control.dirty.hidden=!ui.dirty.has(definition.field);
  }
  for(const definition of inventoryDefinitions){const control=controls.get(definition.field);if(!control)continue;
    const allConfirmed=ui.state?.inputs.every(value=>value.state==='CONFIRMED');control.button.disabled=locked||(!ui.state?.reviews[definition.field]&&(!allConfirmed||(definition.field!=='INCOME'&&!ui.state?.reviews.INCOME)));
  }
  $('analysis-button').disabled=locked;document.querySelectorAll('#analysis-form input,#analysis-form select').forEach(control=>{control.disabled=locked;});
  $('sample-button').disabled=locked;$('result-button').disabled=locked;$('refresh-button').disabled=locked;$('restart-button').disabled=ui.busy||Boolean(ui.pending)||Boolean(ui.reconPending);$('start-button').disabled=ui.busy;
  renderComparisonButtons(locked);
  $('pending-banner').hidden=!ui.pending;$('retry-button').disabled=ui.busy;
}
async function switchTab(name){
  if(!titles[name])return;ui.active=name;
  document.querySelectorAll('[data-tab]').forEach(button=>{button.classList.toggle('active',button.dataset.tab===name);if(button.dataset.tab===name)button.setAttribute('aria-current','page');else button.removeAttribute('aria-current');});
  document.querySelectorAll('.tab-panel').forEach(panel=>{panel.hidden=panel.id!=='panel-'+name;});
  const [breadcrumb,eyebrow,heading,description]=titles[name];$('page-breadcrumb').textContent=breadcrumb;$('page-eyebrow').textContent=eyebrow;$('page-heading').textContent=heading;$('page-description').textContent=description;$('sample-button').hidden=name!=='prepare';
  if(name==='reconciliation'&&ui.state&&!ui.comparisonDirty)await loadComparison();if(name==='history'&&ui.state)await history();if(name==='validation'&&ui.state)await result();
}
async function history(){try{const data=await request('/api/history');const rows=data.events||[];$('history-empty').hidden=rows.length>0;$('history-wrap').hidden=!rows.length;
  $('history-body').replaceChildren(...rows.slice().reverse().map(row=>{const tr=document.createElement('tr');const labels={PROPOSE_AMOUNT:'Amount proposed',CONFIRM_AMOUNT:'Amount confirmed',CLEAR_AMOUNT:'Amount cleared',REVIEW_INVENTORY:'Review completed',UNREVIEW_INVENTORY:'Review reopened',RESOLVE_CONFLICT:'Conflict resolved'};const field=definitions.find(d=>d.field===row.field)?.label||inventoryDefinitions.find(d=>d.field===row.field)?.label||row.field;
    tr.append(element('td','',labels[row.operation]||row.operation),element('td','',field),element('td','',new Intl.DateTimeFormat(undefined,{hour:'2-digit',minute:'2-digit',second:'2-digit'}).format(new Date(row.at))));return tr;}));
  }catch(error){notice(error.message,true);}}
async function result(){try{const data=await request('/api/result');$('result-heading').textContent=data.guide.heading;$('result-message').textContent=data.guide.message;}catch(error){notice(error.message,true);}}
async function seed(){if(blocked())return;ui.filling=true;renderButtons();let completed=0;for(const definition of definitions){const control=controls.get(definition.field);control.input.value=definition.sample;control.unit.value='';ui.dirty.add(definition.field);const ok=await submit({operation:'PROPOSE',field:definition.field,text:definition.sample,unit:''});if(!ok)break;completed++;}ui.filling=false;renderButtons();if(completed===definitions.length)notice('Sample proposals loaded. Review each displayed amount and confirm it explicitly.');}
async function restart(){if(ui.busy||ui.pending||ui.reconPending)return;ui.busy=true;renderButtons();try{if(ui.state)await request('/api/close',{method:'POST',headers:{'X-CSRF-Token':ui.state.csrf_token}});ui.state=null;ui.dirty.clear();for(const definition of definitions){controls.get(definition.field).input.value='';controls.get(definition.field).unit.value='';}await start();await switchTab('prepare');}catch(error){notice(error.message,true);}finally{ui.busy=false;renderButtons();}}
buildComparison();buildPlanningForm();buildInputs();buildReviews();document.querySelectorAll('[data-tab]').forEach(button=>button.addEventListener('click',()=>switchTab(button.dataset.tab)));
$('sample-button').addEventListener('click',seed);$('refresh-button').addEventListener('click',()=>fetchState().catch(error=>notice(error.message,true)));$('restart-button').addEventListener('click',restart);$('start-button').addEventListener('click',start);$('result-button').addEventListener('click',result);$('retry-button').addEventListener('click',()=>{if(ui.pending&&!ui.busy)submit({},ui.pending);});
$('next-button').addEventListener('click',()=>{const next=ui.state?.intake.guide.next_action;if(next?.field&&definitions.some(d=>d.field===next.field)){switchTab('prepare');controls.get(next.field).input.focus();}else if(next?.field==='INCOME_INVENTORY'||next?.field==='NEW_DEDUCTIONS'||next?.field==='OLD_DEDUCTIONS')switchTab('review');else switchTab('validation');});
start();

function clearAnalysis(){const box=$('analysis-results');if(box){box.replaceChildren();box.hidden=true;}}
function buildPlanningForm(){
  const questions=[
    ['screen_business','Business or professional income'],
    ['screen_special','Capital gains, virtual digital assets, lottery or other special-rate income'],
    ['screen_property_foreign','House property, agricultural or foreign income'],
    ['screen_status','Company directorship, unlisted shares, foreign assets/signing authority, deferred ESOP tax or section 194N TDS'],
    ['screen_losses','Brought-forward losses or losses to carry forward'],
    ['screen_adjustments','Family pension, salary exemptions/professional tax, employer NPS or any other income/deduction adjustment']
  ];
  for(const [name,title] of questions){const label=element('label','scope-question');label.append(element('span','',title));const select=document.createElement('select');select.name=name;select.setAttribute('aria-label',title);
    for(const [value,text] of [['UNKNOWN','Select an answer'],['NO','No'],['YES','Yes']]){const option=element('option','',text);option.value=value;select.append(option);}label.append(select);$('scope-questions').append(label);}
  const changed=event=>{clearAnalysis();if(event.target.matches('#plan-80c,#plan-80d,#plan-nps'))$('eligible-confirmed').checked=false;else if(event.target.id!=='eligible-confirmed'&&event.target.id!=='profile-confirmed')$('profile-confirmed').checked=false;};
  $('analysis-form').addEventListener('input',changed);$('analysis-form').addEventListener('change',changed);
  $('analysis-form').addEventListener('submit',event=>{event.preventDefault();checkPlanningScenario();});
}
async function checkPlanningScenario(){
  if(blocked())return;
  const payload={view_token:ui.state.view_token,profile_confirmed:$('profile-confirmed').checked?'YES':'NO',eligible_confirmed:$('eligible-confirmed').checked?'YES':'NO'};
  for(const control of document.querySelectorAll('#analysis-form [name]'))payload[control.name]=control.value;
  ui.busy=true;renderButtons();notice('');clearAnalysis();
  try{
    const data=await request('/api/plan',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8','X-CSRF-Token':ui.state.csrf_token},body:new URLSearchParams(payload).toString()});
    const box=$('analysis-results');const heading=element('h2','', 'Scenario check complete');box.append(heading);
    const issueLabels={PERIOD_NOT_IMPLEMENTED:'This income period is not implemented.',RESIDENCY_REQUIRED:'Select the residential status.',RESIDENCY_OUTSIDE_STARTER:'This residential status is outside the starter.',EXCLUDED_STARTER_FACT:'The selected income or status needs a broader workflow.',SCREENING_INCOMPLETE:'Answer every screening group.',DOB_CONFIRMATION_REQUIRED:'Check and confirm the sample date of birth.',DOB_OUTSIDE_STARTER:'The date of birth is outside this starter’s range.',ADULT_THROUGHOUT_FY_REQUIRED:'The starter requires an adult throughout this financial year.',INCOME_INVENTORY_INCOMPLETE:'Complete income review in Review checkpoints.',CONFIRMED_INCOME_REQUIRED:'Confirm every income input, including intended zeros.',OLD_DEDUCTION_INVENTORY_INCOMPLETE:'Complete the old-regime review checkpoint.',CONFIRMED_ELIGIBLE_DEDUCTIONS_REQUIRED:'Enter and explicitly confirm the eligible deduction amounts.',ELIGIBLE_80D_ABOVE_AGGREGATE_CEILING:'Check the eligible 80D amount; it exceeds the aggregate maximum supported.',SURCHARGE_NOT_IMPLEMENTED:'Rounded total income exceeds the supported range for this regime.'};
    const states={CALCULATED:'Scenario evaluated · arithmetic checks agree',INFORMATION_REQUIRED:'More information needed',UNSUPPORTED:'Outside this starter',VERIFIER_MISMATCH:'Blocked · arithmetic disagreement'};
    for(const [regime,row] of Object.entries(data.checks)){const card=element('div','analysis-row');card.append(element('h3','',regime==='NEW'?'New regime':'Old regime'),element('p','',states[row.input_status]||'Check unavailable'));
      if(row.issues.length){const list=element('ul');for(const code of row.issues)list.append(element('li','',issueLabels[code]||'This scenario needs further review.'));card.append(list);}box.append(card);}
    box.append(element('p','analysis-gate','Tax results remain unavailable. Arithmetic agreement does not establish correct law, deduction eligibility, regime-election availability or independent reference approval.'));
    box.hidden=false;
  }catch(error){notice(error.responseKnown?error.message:'The check could not finish. Refresh the sample and try again.',true);if(error.httpStatus===409){try{await fetchState();}catch(refreshError){notice(refreshError.message,true);}}}
  finally{ui.busy=false;renderButtons();}
}

const statementSample="observation_id,source,category,payer_alias,period,coverage,reference,gross_amount,tds_amount,value_kind\nsalary_manual,MANUAL,SALARY,EMP_A,FY2025-26,ANNUAL,SALARY_TOTAL,800000.00,UNKNOWN,REPORTED\nsalary_form16,FORM16,SALARY,EMP_A,FY2025-26,ANNUAL,SALARY_TOTAL,800000.00,40000.00,REPORTED\nsavings_bank,BANK,SAVINGS_INTEREST,BANK_A,FY2025-26,ANNUAL,SAVINGS_TOTAL,5000.00,UNKNOWN,REPORTED\nsavings_ais,AIS,SAVINGS_INTEREST,BANK_A,FY2025-26,ANNUAL,SAVINGS_TOTAL,6500.00,UNKNOWN,REPORTED\ndeposit_bank,BANK,DEPOSIT_INTEREST,BANK_B,FY2025-26,ANNUAL,DEPOSIT_TOTAL,10000.00,1000.00,REPORTED\ndeposit_ais,AIS,DEPOSIT_INTEREST,BANK_B,FY2025-26,ANNUAL,DEPOSIT_TOTAL,10000.00,1000.00,REPORTED\ndeposit_26as,FORM26AS,DEPOSIT_INTEREST,BANK_B,FY2025-26,ANNUAL,DEPOSIT_TOTAL,UNKNOWN,900.00,REPORTED\ndeposit_repeat,AIS,DEPOSIT_INTEREST,BANK_B,FY2025-26,ANNUAL,DEPOSIT_TOTAL,10000.00,1000.00,REPORTED\n";
function clearComparison(dirty=false){ui.comparison=null;ui.comparisonDirty=dirty;$('comparison-groups').replaceChildren();$('comparison-events').replaceChildren();$('comparison-summary').textContent=dirty?'Input draft or source text changed. Compare the entries again.':'Comparison is separate from your income draft. No automatic totals or tax credits are applied.';}
function buildComparison(){
  $('comparison-csv').addEventListener('input',()=>{clearComparison(true);renderButtons();});
  $('comparison-sample').addEventListener('click',()=>{if(blocked())return;$('comparison-csv').value=statementSample;clearComparison(true);renderButtons();});
  $('comparison-import').addEventListener('click',()=>{if(blocked())return;const body=$('comparison-csv').value;if(new TextEncoder().encode(body).length>32768){notice('Keep the normalized CSV under 32 KiB.',true);return;}
    comparisonAction({path:'/api/reconciliation/import',body,headers:{'Content-Type':'text/csv;charset=UTF-8','X-Request-ID':crypto.randomUUID(),'X-Input-View':ui.state.view_token}});});
  $('comparison-retry').addEventListener('click',()=>{if(ui.reconPending&&!ui.busy)comparisonAction(ui.reconPending);});
}
function renderComparisonButtons(locked){
  $('comparison-import').disabled=locked;$('comparison-sample').disabled=locked;$('comparison-csv').disabled=locked;
  $('comparison-pending').hidden=!ui.reconPending;$('comparison-retry').disabled=ui.busy;
  for(const card of document.querySelectorAll('.comparison-group')){const choose=card.querySelector('.observation-choice'),reason=card.querySelector('.review-reason');const invalid=locked||ui.comparisonDirty;
    choose.disabled=invalid;reason.disabled=invalid;card.querySelector('.record-review').disabled=invalid||!choose.value||!reason.value;card.querySelector('.reopen-review').disabled=invalid||card.dataset.reviewed!=='true';
    const recorded=card.dataset.reviewed==='true'&&choose.value===card.dataset.selected&&reason.value===card.dataset.reason;
    const proposalBlocked=invalid||!recorded||card.dataset.proposalReady!=='true'||ui.dirty.size>0;
    for(const box of card.querySelectorAll('.source-ack'))box.disabled=proposalBlocked;
    card.querySelector('.create-source-proposal').disabled=proposalBlocked||![...card.querySelectorAll('.source-ack')].every(box=>box.checked);}
}
async function loadComparison(){
  if(ui.comparisonDirty)return;
  try{ui.comparison=await request('/api/reconciliation');renderComparison();return true;}
  catch(error){clearComparison(error.httpStatus===409);notice(error.httpStatus===409?'The comparison belongs to an earlier income draft. Compare the source entries again.':error.message,true);return false;}
}
function renderComparison(){
  const data=ui.comparison;$('comparison-groups').replaceChildren();$('comparison-events').replaceChildren();
  if(!data||data.status==='NO_COMPARISON'){$('comparison-summary').textContent='No comparison yet. Load or paste normalized sample entries and compare them.';return;}
  $('comparison-summary').textContent=data.groups_reviewed+' of '+data.group_count+' groups have a local source selection. This is not independent approval or filing readiness.';
  const statusNames={AGREEMENT:'Amounts agree',AMOUNT_CONFLICT:'Amounts differ',VALUE_MISSING:'Value missing',SINGLE_SOURCE:'Only one source',REPEATED_SOURCE_OBSERVATIONS:'Repeated source observations',SOURCE_VARIANTS:'Different source value kinds'};
  const reasons=[['CHECKED_SOURCE_RECORD','Checked the source record'],['CORRECTED_CLASSIFICATION','Checked the classification'],['DUPLICATE_REPRESENTATION','Reviewed duplicate representation'],['INDEPENDENT_WORKING','Compared with separate workings']];
  for(const group of data.groups){const card=element('article','comparison-group');card.dataset.reviewed=String(group.local_review==='OBSERVATION_SELECTED');card.dataset.selected=group.selected_observation_id||'';card.dataset.reason=group.review_reason||'';card.dataset.proposalReady=String(group.proposal_policy==='READY_FOR_USER_ATTESTATION');
    card.append(element('h3','',group.category.replaceAll('_',' ')+' · '+group.payer_alias),element('p','comparison-context',group.period+' · '+group.coverage+' · '+group.reference));
    const checks=element('div','comparison-checks');checks.append(element('span','', 'Gross: '+group.income_flags.map(v=>statusNames[v]).join(' · ')),element('span','', 'TDS: '+group.tds_flags.map(v=>statusNames[v]).join(' · ')));card.append(checks);
    const wrap=element('div','table-wrap');const table=document.createElement('table');const head=document.createElement('thead');const header=document.createElement('tr');for(const label of ['Source / value kind','Gross input','TDS input'])header.append(element('th','',label));head.append(header);const body=document.createElement('tbody');
    for(const row of group.observations){const tr=document.createElement('tr');tr.append(element('td','',row.source+' / '+row.value_kind),element('td','',row.gross_amount==='UNKNOWN'?'Unknown':money(row.gross_amount)),element('td','',row.tds_amount==='UNKNOWN'?'Unknown':money(row.tds_amount)));body.append(tr);}table.append(head,body);wrap.append(table);card.append(wrap);
    const form=element('div','comparison-review');const choose=document.createElement('select');choose.className='observation-choice';choose.setAttribute('aria-label','Source observation for '+group.payer_alias);const empty=element('option','','Choose an observation');empty.value='';choose.append(empty);
    for(const row of group.observations){const option=element('option','',row.observation_id+' · '+row.source+' · '+(row.gross_amount==='UNKNOWN'?'gross unknown':money(row.gross_amount)));option.value=row.observation_id;option.disabled=row.gross_amount==='UNKNOWN';choose.append(option);}choose.value=group.selected_observation_id||'';
    const reason=document.createElement('select');reason.className='review-reason';reason.setAttribute('aria-label','Review reason for '+group.payer_alias);const first=element('option','','Choose a review reason');first.value='';reason.append(first);for(const [value,label] of reasons){const option=element('option','',label);option.value=value;reason.append(option);}reason.value=group.review_reason||'';
    const save=element('button','button small record-review','Record local review');save.type='button';const reopen=element('button','secondary-button reopen-review','Reopen');reopen.type='button';
    const send=(operation)=>{if(blocked()||ui.comparisonDirty)return;const values={operation,request_id:crypto.randomUUID(),view_token:ui.state.view_token,comparison_token:ui.comparison.comparison_token,group_id:group.group_id};if(operation==='SELECT'){values.observation_id=choose.value;values.reason=reason.value;}comparisonAction({path:'/api/reconciliation/review',body:new URLSearchParams(values).toString(),headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'}});};
    save.addEventListener('click',()=>send('SELECT'));reopen.addEventListener('click',()=>send('REOPEN'));const changed=()=>{for(const box of card.querySelectorAll('.source-ack'))box.checked=false;renderButtons();};choose.addEventListener('change',changed);reason.addEventListener('change',changed);
    form.append(choose,reason,save,reopen);card.append(form,element('p','comparison-help',group.local_review==='OBSERVATION_SELECTED'?'Local source choice recorded. Original differences remain visible.':'No observation is selected automatically.'));
    const proposal=element('div','source-proposal');const selected=group.observations.find(row=>row.observation_id===group.selected_observation_id);const target=definitions.find(row=>row.field===group.proposal_field);
    proposal.append(element('p','',target&&selected?'Propose '+money(selected.gross_amount)+' as the whole '+target.label.toLowerCase()+'. This replaces the current input and still needs separate confirmation.':'Record a supported annual source observation to prepare an income proposal.'));
    const policyLabels={READY_FOR_USER_ATTESTATION:'Check annual coverage and classification before creating a proposal. TDS is not transferred.',CATEGORY_NOT_PLANNER_INPUT:'This category needs a broader workflow.',SOURCE_PERIOD_NOT_SUPPORTED:'This source period is outside the local sample.',ANNUAL_SOURCE_REQUIRED:'Transaction entries cannot replace annual income inputs.',CATEGORY_COVERAGE_AMBIGUOUS:'Multiple groups cover this category. Prepare and check the annual total separately.',GROSS_SOURCE_REVIEW_REQUIRED:'Missing, conflicting, repeated or variant gross observations block source transfer.'};
    proposal.append(element('p','comparison-help',policyLabels[group.proposal_policy]||'Source proposal unavailable.'));
    for(const [name,text] of [['whole','I checked that this is the complete annual category, including all payers, and that the classification and qualifying-interest conditions apply.'],['replace','Replace the current category input with this gross amount. Do not add it to the existing input.']]){const label=element('label','check-label');const box=document.createElement('input');box.type='checkbox';box.className='source-ack source-ack-'+name;box.addEventListener('change',renderButtons);label.append(box,document.createTextNode(text));proposal.append(label);}
    const create=element('button','button small create-source-proposal','Create income proposal');create.type='button';create.addEventListener('click',()=>{if(create.disabled||blocked()||!selected||!target)return;const values={request_id:crypto.randomUUID(),view_token:ui.state.view_token,comparison_token:ui.comparison.comparison_token,group_id:group.group_id,observation_id:selected.observation_id,displayed_amount:selected.gross_amount,field:group.proposal_field,whole_category_confirmed:'YES',replace_input_confirmed:'YES'};comparisonAction({path:'/api/reconciliation/propose',field:group.proposal_field,body:new URLSearchParams(values).toString(),headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'}});});proposal.append(create);card.append(proposal);$('comparison-groups').append(card);
  }
  for(const event of data.events)$('comparison-events').append(element('li','',event.operation.replaceAll('_',' ')+' · '+new Date(event.at).toLocaleTimeString()));renderButtons();
}
async function comparisonAction(action){
  ui.busy=true;renderButtons();let accepted=false;
  try{const receipt=await request(action.path,{method:'POST',headers:{...action.headers,'X-CSRF-Token':ui.state.csrf_token},body:action.body});accepted=true;
    if(action.path==='/api/reconciliation/propose'){
      await fetchState();ui.reconPending=null;ui.comparisonDirty=true;clearComparison(true);
      const control=controls.get(action.field);if(control){control.input.value='';control.unit.value='';}await switchTab('prepare');
      notice(receipt.status==='REPLAYED'?'The earlier source request was already accepted. The current draft is shown; later edits were preserved.':'Source amount proposed as a replacement. Review the current value and use Confirm amount. Compare sources again after income changes.');
    }else{ui.reconPending=null;ui.comparisonDirty=false;if(receipt.status==='REPLAYED')$('comparison-csv').value='';const loaded=await loadComparison();if(loaded)notice(receipt.status==='REPLAYED'?'The earlier action was already accepted. The current comparison was refreshed.':'Local comparison updated. Confirmed income and tax permissions are unchanged.');else notice('The action was accepted, but the comparison could not refresh. Open this tab again.',true);}
  }catch(error){if(error.httpStatus===401){ui.reconPending=null;ui.state=null;$('session-ended').hidden=false;$('workspace-content').hidden=true;notice(error.message,true);}
    else if(accepted||!error.responseKnown){ui.reconPending=action;notice(accepted?'The source action was accepted, but the current draft could not refresh. Retry the exact action to refresh safely.':'The action outcome is unknown. Retry the exact same action before making other changes.',true);}
    else{ui.reconPending=null;notice(errors[error.data?.status]||error.message,true);if(error.httpStatus===409){try{await fetchState();ui.comparisonDirty=false;await loadComparison();}catch(refreshError){notice(refreshError.message,true);}}}
  }finally{ui.busy=false;renderButtons();}
}
