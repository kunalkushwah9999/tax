#!/usr/bin/env python3
"""Actual loopback HTTP integration checks for the sample application; no external target."""
from pathlib import Path
import concurrent.futures
import hashlib
import http.cookiejar
import json
import queue
import subprocess
import sys
import threading
import unittest
import urllib.error
import urllib.parse
import urllib.request
import uuid

ROOT=Path(__file__).resolve().parent
BASE=''

class SessionClient:
    def __init__(self,bootstrap=True):
        self.cookies=http.cookiejar.CookieJar()
        self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies))
        self.state=None
        if bootstrap:self.state=self.call('/api/session')[1]
    def call(self,path,method='GET',body=None,headers=None,status=200):
        values={}
        if method=='POST' and self.state:
            values={'Origin':BASE,'X-CSRF-Token':self.state['csrf_token']}
        if isinstance(body,dict):body=urllib.parse.urlencode(body).encode();values['Content-Type']='application/x-www-form-urlencoded;charset=UTF-8'
        for name,value in (headers or {}).items():
            if value is None:values.pop(name,None)
            else:values[name]=value
        request=urllib.request.Request(BASE+path,data=body,headers=values,method=method)
        try:response=self.opener.open(request,timeout=5)
        except urllib.error.HTTPError as error:response=error
        with response:
            raw=response.read();ctype=response.headers.get('Content-Type','')
            data=json.loads(raw) if 'application/json' in ctype else raw.decode()
            if response.status!=status:raise AssertionError('unexpected HTTP '+str(response.status)+' for '+path+'; expected '+str(status))
            return response.headers,data
    def refresh(self):self.state=self.call('/api/state')[1];return self.state
    def payload(self,operation,**fields):return dict(operation=operation,request_id=str(uuid.uuid4()),view_token=self.state['view_token'],**fields)
    def edit(self,operation,expected_status=200,**fields):
        payload=self.payload(operation,**fields);response=self.call('/api/edit','POST',payload,status=expected_status)[1]
        if expected_status==200:self.refresh()
        return response
    def input(self,field):return next(x for x in self.state['inputs'] if x['field']==field)

class HTTPTests(unittest.TestCase):
    def test_static_page_and_assets_have_explicit_security_headers(self):
        b=SessionClient(False)
        for path in ['/','/app.js','/styles.css']:
            headers,body=b.call(path)
            self.assertEqual(headers['Cache-Control'],'no-store');self.assertEqual(headers['X-Content-Type-Options'],'nosniff')
            self.assertEqual(headers['X-Frame-Options'],'DENY');self.assertIn("frame-ancestors 'none'",headers['Content-Security-Policy'])
            self.assertTrue(body)
    def test_session_cookie_is_opaque_http_only_host_only_and_same_site_strict(self):
        b=SessionClient(False);headers,data=b.call('/api/session');cookie=headers['Set-Cookie']
        self.assertIn('HttpOnly',cookie);self.assertIn('SameSite=Strict',cookie);self.assertNotIn('Domain=',cookie)
        self.assertEqual(data['mode'],'SYNTHETIC_PREPARATION_DEMO');self.assertFalse(data['saved_for_later'])
        self.assertTrue(all(x['state']=='UNKNOWN' and 'amount' not in x for x in data['inputs']))
    def test_api_reads_require_the_current_session_cookie(self):
        b=SessionClient(False)
        for path in ['/api/state','/api/history','/api/result']:
            self.assertEqual(b.call(path,status=401)[1]['status'],'SESSION_REQUIRED')
    def test_wrong_host_is_rejected_before_bootstrap(self):
        b=SessionClient(False);self.assertEqual(b.call('/api/session',headers={'Host':'untrusted.invalid'},status=421)[1]['status'],'HOST_NOT_ALLOWED')
    def test_cross_origin_bootstrap_is_rejected(self):
        b=SessionClient(False);self.assertEqual(b.call('/api/session',headers={'Origin':'https://untrusted.invalid'},status=403)[1]['status'],'ORIGIN_NOT_ALLOWED')
    def test_fetch_metadata_cross_site_request_is_rejected(self):
        b=SessionClient(False);self.assertEqual(b.call('/api/session',headers={'Sec-Fetch-Site':'cross-site'},status=403)[1]['status'],'CROSS_SITE_REQUEST')
    def test_csrf_wrong_token_cannot_edit(self):
        b=SessionClient();p=b.payload('CLEAR',field='SALARY')
        self.assertEqual(b.call('/api/edit','POST',p,headers={'X-CSRF-Token':'wrong'},status=403)[1]['status'],'CSRF_REJECTED')
        self.assertEqual(b.refresh()['inputs'][0]['state'],'UNKNOWN')
    def test_post_origin_is_required(self):
        b=SessionClient();p=b.payload('CLEAR',field='SALARY')
        self.assertEqual(b.call('/api/edit','POST',p,headers={'Origin':None},status=403)[1]['status'],'ORIGIN_REQUIRED')
        self.assertEqual(b.call('/api/edit','POST',p,headers={'Origin':''},status=403)[1]['status'],'ORIGIN_NOT_ALLOWED')
    def test_missing_csrf_token_cannot_edit(self):
        b=SessionClient();p=b.payload('CLEAR',field='SALARY')
        self.assertEqual(b.call('/api/edit','POST',p,headers={'X-CSRF-Token':None},status=403)[1]['status'],'CSRF_REJECTED')
    def test_duplicate_cookie_does_not_pick_a_session(self):
        b=SessionClient();cookie='; '.join(x.name+'='+x.value for x in b.cookies)
        self.assertEqual(b.call('/api/state',headers={'Cookie':cookie+'; '+cookie},status=400)[1]['status'],'DUPLICATE_SESSION_COOKIE')
    def test_standard_form_charset_whitespace_is_accepted(self):
        b=SessionClient();p=b.payload('PROPOSE',field='SALARY',text='1 lakh',unit='')
        self.assertEqual(b.call('/api/edit','POST',p,headers={'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'})[1]['status'],'APPLIED')
        self.assertEqual(b.refresh()['inputs'][0]['amount'],'100000.00')
    def test_simple_cross_origin_form_does_not_bypass_csrf(self):
        b=SessionClient();p=b.payload('CLEAR',field='SALARY')
        self.assertEqual(b.call('/api/edit','POST',p,headers={'Origin':'https://untrusted.invalid'},status=403)[1]['status'],'ORIGIN_NOT_ALLOWED')
    def test_wrong_method_is_rejected(self):
        b=SessionClient();self.assertEqual(b.call('/api/edit',status=405)[1]['status'],'METHOD_NOT_ALLOWED')
    def test_json_request_bodies_are_not_bound_to_trusted_domain_objects(self):
        b=SessionClient();self.assertEqual(b.call('/api/edit','POST',b'{}',headers={'Content-Type':'application/json'},status=415)[1]['status'],'FORM_CONTENT_TYPE_REQUIRED')
    def test_oversized_body_is_rejected(self):
        b=SessionClient();self.assertEqual(b.call('/api/edit','POST',b'x'*4097,headers={'Content-Type':'application/x-www-form-urlencoded'},status=413)[1]['status'],'REQUEST_TOO_LARGE')
    def test_duplicate_unknown_fields_and_mass_assignment_are_rejected(self):
        b=SessionClient();p=b.payload('CLEAR',field='SALARY');raw=urllib.parse.urlencode(p).encode()
        self.assertEqual(b.call('/api/edit','POST',raw+b'&field=OTHER_INTEREST',headers={'Content-Type':'application/x-www-form-urlencoded'},status=400)[1]['status'],'INVALID_REQUEST')
        for name in ['tenant_id','scope','reference_state','paid_plan','authorized','calculated_tax']:
            self.assertEqual(b.call('/api/edit','POST',dict(p,**{name:'APPROVED'}),status=400)[1]['status'],'UNEXPECTED_FIELDS')
        self.assertEqual(b.refresh()['inputs'][0]['state'],'UNKNOWN')
    def test_invalid_utf8_and_percent_escapes_are_rejected(self):
        b=SessionClient()
        for raw in [b'x=%FF',b'x=%C0%AF',b'x=%GG',b'x=%',b'x=\xff','x=%ＦＦ'.encode()]:
            self.assertEqual(b.call('/api/edit','POST',raw,headers={'Content-Type':'application/x-www-form-urlencoded'},status=400)[1]['status'],'INVALID_REQUEST')
    def test_sample_proposal_is_exact_estimated_and_not_confirmed(self):
        b=SessionClient();self.assertEqual(b.edit('PROPOSE',field='SALARY',text='1 lakh',unit='')['status'],'APPLIED')
        self.assertEqual(b.input('SALARY'),{'field':'SALARY','state':'ESTIMATED','amount':'100000.00'})
        self.assertFalse(b.state['reviews']['INCOME'])
    def test_blank_is_rejected_and_does_not_clear_existing_amount(self):
        b=SessionClient();b.edit('PROPOSE',field='SALARY',text='1 lakh',unit='')
        result=b.edit('PROPOSE',expected_status=400,field='SALARY',text='',unit='')
        self.assertEqual(result['input_problem'],'MISSING');self.assertEqual(b.refresh()['inputs'][0]['amount'],'100000.00')
    def test_wrong_displayed_confirmation_value_is_rejected(self):
        b=SessionClient();b.edit('PROPOSE',field='SALARY',text='1 lakh',unit='')
        self.assertEqual(b.edit('CONFIRM',expected_status=409,field='SALARY',amount='200000.00')['status'],'CONFIRMATION_VALUE_CHANGED')
        self.assertEqual(b.refresh()['inputs'][0]['state'],'ESTIMATED')
    def test_exact_confirmation_and_clear_preserve_unknown_zero_distinction(self):
        b=SessionClient();b.edit('PROPOSE',field='SAVINGS_INTEREST',text='0 rupees',unit='');b.edit('CONFIRM',field='SAVINGS_INTEREST',amount='0.00')
        self.assertEqual(b.input('SAVINGS_INTEREST')['state'],'CONFIRMED');self.assertEqual(b.input('SAVINGS_INTEREST')['amount'],'0.00')
        b.edit('CLEAR',field='SAVINGS_INTEREST');self.assertEqual(b.input('SAVINGS_INTEREST')['state'],'UNKNOWN');self.assertNotIn('amount',b.input('SAVINGS_INTEREST'))
    def test_a_stale_browser_view_cannot_overwrite_a_newer_edit(self):
        b=SessionClient();stale=b.payload('PROPOSE',field='SALARY',text='2 lakh',unit='');b.edit('PROPOSE',field='SALARY',text='1 lakh',unit='')
        self.assertEqual(b.call('/api/edit','POST',stale,status=409)[1]['status'],'STALE_INPUT');self.assertEqual(b.refresh()['inputs'][0]['amount'],'100000.00')
    def test_identical_http_retry_replays_once_even_after_later_edits(self):
        b=SessionClient();p=b.payload('PROPOSE',field='SALARY',text='1 lakh',unit='');first=b.call('/api/edit','POST',p)[1];b.refresh()
        b.edit('PROPOSE',field='OTHER_INTEREST',text='2 rupees',unit='');latest=b.state['view_token']
        replay=b.call('/api/edit','POST',p)[1];self.assertEqual(replay['status'],'REPLAYED');self.assertFalse(replay['state_changed'])
        self.assertEqual(replay['outcome_snapshot_id'],first['outcome_snapshot_id']);self.assertTrue(replay['refresh_current_draft'])
        self.assertEqual(b.refresh()['view_token'],latest);self.assertEqual(len(b.call('/api/history')[1]['events']),2)
    def test_same_id_with_a_changed_http_body_is_a_conflict(self):
        b=SessionClient();p=b.payload('PROPOSE',field='SALARY',text='1 lakh',unit='');b.call('/api/edit','POST',p)
        self.assertEqual(b.call('/api/edit','POST',dict(p,text='2 lakh'),status=409)[1]['status'],'IDEMPOTENCY_CONFLICT')
    def test_other_session_view_tokens_cannot_edit_this_session(self):
        a=SessionClient();b=SessionClient();p=b.payload('PROPOSE',field='SALARY',text='1 lakh',unit='');p['view_token']=a.state['view_token']
        self.assertEqual(b.call('/api/edit','POST',p,status=409)[1]['status'],'STALE_INPUT')
        self.assertTrue(all(x['state']=='UNKNOWN' for x in a.refresh()['inputs']));self.assertTrue(all(x['state']=='UNKNOWN' for x in b.refresh()['inputs']))
    def test_income_review_cannot_be_completed_before_confirmation(self):
        b=SessionClient();self.assertEqual(b.edit('REVIEW',expected_status=409,inventory='INCOME')['status'],'INPUT_REVIEW_INCOMPLETE')
    def test_complete_http_flow_reaches_eight_reviews_without_a_tax_result(self):
        b=SessionClient()
        values={'SALARY':'800000.00','SAVINGS_INTEREST':'5000.00','DEPOSIT_INTEREST':'0.00','OTHER_INTEREST':'0.00'}
        for field,value in values.items():b.edit('PROPOSE',field=field,text=value,unit='RUPEES');b.edit('CONFIRM',field=field,amount=value)
        for inventory in ['INCOME','NEW_DEDUCTIONS','OLD_DEDUCTIONS']:b.edit('REVIEW',inventory=inventory)
        guide=b.state['intake']['guide'];self.assertEqual(guide['progress_label'],'8 of 8 input reviews complete');self.assertEqual(guide['next_action']['action'],'WAIT_FOR_VALIDATION')
        response=b.call('/api/result')[1];self.assertEqual(response['result']['status'],'BLOCKED');self.assertNotIn('annual_taxes',response['result']);self.assertNotIn('comparison',response['result'])
        self.assertFalse(response['result']['capabilities']['submit_return']);self.assertFalse(response['result']['capabilities']['export_itr'])
        b.edit('PROPOSE',field='SALARY',text='9 lakh',unit='');self.assertFalse(any(b.state['reviews'].values()))
    def test_history_excludes_amounts_raw_strings_and_session_actors(self):
        b=SessionClient();b.edit('PROPOSE',field='SALARY',text='1 lakh',unit='');history=b.call('/api/history')[1];serialized=json.dumps(history)
        self.assertEqual(len(history['events']),1)
        for item in ['100000.00','1 lakh','csrf_token','view_token','local-demo']:self.assertNotIn(item,serialized)
    def test_close_revokes_the_cookie_session_and_old_retry(self):
        b=SessionClient();p=b.payload('PROPOSE',field='SALARY',text='1 lakh',unit='');b.call('/api/edit','POST',p)
        self.assertEqual(b.call('/api/close','POST',b'')[1]['status'],'SESSION_CLOSED')
        self.assertEqual(b.call('/api/state',status=401)[1]['status'],'SESSION_REQUIRED')
        self.assertEqual(b.call('/api/edit','POST',p,status=401)[1]['status'],'SESSION_REQUIRED')
    def test_static_path_traversal_query_and_unadvertised_actions_are_rejected(self):
        b=SessionClient()
        for path in ['/../README.md','/%2e%2e/README.md','/src/main/java','/api/approve']:
            self.assertEqual(b.call(path,status=404)[1]['status'],'NOT_FOUND')
        self.assertEqual(b.call('/api/state?paid=true',status=400)[1]['status'],'QUERY_NOT_ALLOWED')
    def test_concurrent_http_retries_share_one_accepted_transition(self):
        b=SessionClient();p=b.payload('PROPOSE',field='SALARY',text='1 lakh',unit='');cookie='; '.join(x.name+'='+x.value for x in b.cookies)
        def call():
            local=SessionClient(False);return local.call('/api/edit','POST',p,headers={'Cookie':cookie,'Origin':BASE,'X-CSRF-Token':b.state['csrf_token']})[1]['status']
        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:statuses=list(pool.map(lambda _:call(),range(4)))
        self.assertEqual(statuses.count('APPLIED'),1);self.assertEqual(statuses.count('REPLAYED'),3);self.assertEqual(len(b.call('/api/history')[1]['events']),1)


    @staticmethod
    def prepared(salary='800000.00'):
        b=SessionClient()
        for field,value in {'SALARY':salary,'SAVINGS_INTEREST':'5000.00','DEPOSIT_INTEREST':'0.00','OTHER_INTEREST':'0.00'}.items():
            b.edit('PROPOSE',field=field,text=value,unit='RUPEES');b.edit('CONFIRM',field=field,amount=value)
        for inventory in ['INCOME','NEW_DEDUCTIONS','OLD_DEDUCTIONS']:b.edit('REVIEW',inventory=inventory)
        return b
    @staticmethod
    def profile(b,**changes):
        p={'view_token':b.state['view_token'],'period':'ITA1961-FY2025-26-AY2026-27','dob':'2000-10-15','residency':'ROR',
           'eligible_80c':'0.00','eligible_80d':'0.00','eligible_nps':'0.00','profile_confirmed':'YES','eligible_confirmed':'YES',
           'screen_business':'NO','screen_special':'NO','screen_property_foreign':'NO','screen_status':'NO','screen_losses':'NO','screen_adjustments':'NO','regimes':'BOTH'}
        return dict(p,**changes)
    def test_planning_arithmetic_matches_but_public_values_stay_blocked(self):
        b=self.prepared();data=b.call('/api/plan','POST',self.profile(b))[1]
        for regime in ['NEW','OLD']:
            self.assertEqual(data['checks'][regime]['input_status'],'CALCULATED');self.assertEqual(data['checks'][regime]['arithmetic_cross_check'],'MATCH')
        self.assertEqual(data['result']['status'],'BLOCKED');self.assertFalse(data['calculation_values_released']);self.assertEqual(data['independent_approved_cases'],0)
        self.assertNotIn('comparison',data['result']);self.assertFalse(data['result']['capabilities']['show_annual_tax']);self.assertFalse(data['result']['capabilities']['show_definitive_comparison'])
        text=json.dumps(data)
        for secret in ['800000.00','5000.00','2000-10-15','annual_taxes','annual_tax_before_credits','slab_tax','workings']:self.assertNotIn(secret,text)
    def test_planning_unknown_income_cannot_become_zero(self):
        b=SessionClient();data=b.call('/api/plan','POST',self.profile(b))[1]
        self.assertTrue(all(r['input_status']=='INFORMATION_REQUIRED' for r in data['checks'].values()));self.assertFalse(data['calculation_values_released'])
    def test_planning_profile_confirmation_is_explicit(self):
        b=self.prepared();data=b.call('/api/plan','POST',self.profile(b,profile_confirmed='NO'))[1]
        self.assertIn('DOB_CONFIRMATION_REQUIRED',data['checks']['NEW']['issues'])
    def test_planning_unknown_deductions_do_not_produce_old_zero(self):
        b=self.prepared();data=b.call('/api/plan','POST',self.profile(b,eligible_80d=''))[1]
        self.assertEqual(data['checks']['OLD']['input_status'],'INFORMATION_REQUIRED');self.assertEqual(data['checks']['NEW']['input_status'],'CALCULATED')
    def test_planning_deductions_must_be_confirmed_not_just_typed(self):
        b=self.prepared();data=b.call('/api/plan','POST',self.profile(b,eligible_confirmed='NO'))[1]
        self.assertIn('CONFIRMED_ELIGIBLE_DEDUCTIONS_REQUIRED',data['checks']['OLD']['issues'])
    def test_planning_outside_period_and_residency_are_explicit(self):
        b=self.prepared()
        for fields,issue in [({'period':'ITA2025-TY2026-27'},'PERIOD_NOT_IMPLEMENTED'),({'residency':'RNOR'},'RESIDENCY_OUTSIDE_STARTER')]:
            data=b.call('/api/plan','POST',self.profile(b,**fields))[1];self.assertIn(issue,data['checks']['NEW']['issues']);self.assertFalse(data['calculation_values_released'])
    def test_planning_screening_unknown_and_excluded_do_not_assume_supported(self):
        b=self.prepared()
        for answer,status in [('UNKNOWN','INFORMATION_REQUIRED'),('YES','UNSUPPORTED')]:
            data=b.call('/api/plan','POST',self.profile(b,screen_special=answer))[1];self.assertEqual(data['checks']['NEW']['input_status'],status)
    def test_planning_bad_calendar_and_money_are_safe_client_errors(self):
        b=self.prepared()
        for fields in [{'dob':'2000-02-30'},{'eligible_80c':'1e3'},{'eligible_80d':'-1.00'},{'profile_confirmed':'APPROVED'}]:
            self.assertEqual(b.call('/api/plan','POST',self.profile(b,**fields),status=400)[1]['status'],'INVALID_REQUEST')
        self.assertTrue(all(x['state']=='CONFIRMED' for x in b.refresh()['inputs']))
    def test_planning_authority_mass_assignment_and_duplicates_rejected(self):
        b=SessionClient();p=self.profile(b)
        for field in ['reference_state','rule_state','release_approved','tenant_id','calculated_tax']:
            self.assertEqual(b.call('/api/plan','POST',dict(p,**{field:'APPROVED'}),status=400)[1]['status'],'UNEXPECTED_FIELDS')
        raw=urllib.parse.urlencode(p).encode()+b'&dob=2001-01-01'
        self.assertEqual(b.call('/api/plan','POST',raw,headers={'Content-Type':'application/x-www-form-urlencoded'},status=400)[1]['status'],'INVALID_REQUEST')
    def test_planning_requires_origin_csrf_and_post(self):
        b=SessionClient();p=self.profile(b)
        for fields in [{'X-CSRF-Token':None},{'Origin':None},{'Origin':'https://untrusted.invalid'}]:b.call('/api/plan','POST',p,headers=fields,status=403)
        self.assertEqual(b.call('/api/plan',status=405)[1]['status'],'METHOD_NOT_ALLOWED')
    def test_planning_stale_or_other_session_view_cannot_rebind_inputs(self):
        b=self.prepared();p=self.profile(b);b.edit('PROPOSE',field='SALARY',text='9 lakh',unit='')
        self.assertEqual(b.call('/api/plan','POST',p,status=409)[1]['status'],'STALE_INPUT')
        other=SessionClient();self.assertEqual(other.call('/api/plan','POST',p,status=409)[1]['status'],'STALE_INPUT')
    def test_planning_read_does_not_change_session_facts_or_edit_history(self):
        b=self.prepared();before=b.state;events=b.call('/api/history')[1]
        first=b.call('/api/plan','POST',self.profile(b))[1];second=b.call('/api/plan','POST',self.profile(b))[1]
        self.assertEqual(first,second);self.assertEqual(b.refresh(),before);self.assertEqual(b.call('/api/history')[1],events)
    def test_planning_income_cap_is_per_regime_after_salary_deduction(self):
        b=self.prepared('5070000.00');data=b.call('/api/plan','POST',self.profile(b))[1]
        self.assertEqual(data['checks']['NEW']['input_status'],'CALCULATED');self.assertEqual(data['checks']['OLD']['input_status'],'UNSUPPORTED')
    def test_planning_request_decoding_and_size_are_bounded(self):
        b=SessionClient()
        b.call('/api/plan','POST',b'{}',headers={'Content-Type':'application/json'},status=415)
        b.call('/api/plan','POST',b'x'*4097,headers={'Content-Type':'application/x-www-form-urlencoded'},status=413)
        b.call('/api/plan','POST',b'x=%FF',headers={'Content-Type':'application/x-www-form-urlencoded'},status=400)
    def test_planning_session_limit_is_enforced_without_public_amounts(self):
        b=SessionClient();p=self.profile(b)
        for _ in range(100):b.call('/api/plan','POST',p)
        self.assertEqual(b.call('/api/plan','POST',p,status=429)[1]['status'],'PLANNING_LIMIT_REACHED')


    @staticmethod
    def statement_data():return (ROOT/'examples/statement_comparison.csv').read_bytes()
    @staticmethod
    def import_headers(b,request_id=None):return {'Content-Type':'text/csv;charset=UTF-8','X-Request-ID':request_id or str(uuid.uuid4()),'X-Input-View':b.state['view_token']}
    def imported(self):
        b=SessionClient();b.call('/api/reconciliation/import','POST',self.statement_data(),headers=self.import_headers(b));return b,b.call('/api/reconciliation')[1]
    @staticmethod
    def source_choice(b,data,group=None,observation=None):
        group=group or next(g for g in data['groups'] if g['category']=='SAVINGS_INTEREST')
        return {'operation':'SELECT','request_id':str(uuid.uuid4()),'view_token':b.state['view_token'],'comparison_token':data['comparison_token'],'group_id':group['group_id'],'observation_id':observation or group['observations'][0]['observation_id'],'reason':'CHECKED_SOURCE_RECORD'}
    def test_comparison_starts_empty_and_requires_authentication(self):
        b=SessionClient();self.assertEqual(b.call('/api/reconciliation')[1]['status'],'NO_COMPARISON')
        SessionClient(False).call('/api/reconciliation',status=401)
    def test_comparison_import_preserves_input_observations_and_all_flags(self):
        b,data=self.imported();self.assertEqual(data['group_count'],3);self.assertEqual(data['groups_reviewed'],0)
        self.assertFalse(data['filing_ready']);self.assertFalse(data['income_facts_modified']);self.assertFalse(data['source_authenticity_verified'])
        deposit=next(g for g in data['groups'] if g['category']=='DEPOSIT_INTEREST')
        self.assertEqual(len(deposit['observations']),4);self.assertIn('VALUE_MISSING',deposit['income_flags']);self.assertIn('REPEATED_SOURCE_OBSERVATIONS',deposit['income_flags']);self.assertIn('AMOUNT_CONFLICT',deposit['tds_flags'])
    def test_comparison_selection_never_changes_income_or_tax_capabilities(self):
        b,data=self.imported();before=b.refresh();events=b.call('/api/history')[1]
        b.call('/api/reconciliation/review','POST',self.source_choice(b,data));updated=b.call('/api/reconciliation')[1]
        self.assertEqual(updated['groups_reviewed'],1);self.assertEqual(before,b.refresh());self.assertEqual(events,b.call('/api/history')[1])
        self.assertEqual(b.call('/api/result')[1]['result']['status'],'BLOCKED')
    def test_comparison_unknown_gross_cannot_be_selected_as_zero(self):
        b,data=self.imported();g=next(g for g in data['groups'] if g['category']=='DEPOSIT_INTEREST')
        self.assertEqual(b.call('/api/reconciliation/review','POST',self.source_choice(b,data,g,'deposit_26as'),status=409)[1]['status'],'GROSS_VALUE_MISSING')
    def test_comparison_source_selection_is_group_bound(self):
        b,data=self.imported();self.assertEqual(b.call('/api/reconciliation/review','POST',self.source_choice(b,data,observation='salary_manual'),status=409)[1]['status'],'OBSERVATION_NOT_FOUND')
    def test_comparison_old_versions_and_other_sessions_cannot_rebind_reviews(self):
        b,data=self.imported();first=self.source_choice(b,data);b.call('/api/reconciliation/review','POST',first)
        self.assertEqual(b.call('/api/reconciliation/review','POST',self.source_choice(b,data),status=409)[1]['status'],'STALE_RECONCILIATION')
        other,foreign=self.imported();p=self.source_choice(other,foreign);p['comparison_token']=data['comparison_token']
        self.assertEqual(other.call('/api/reconciliation/review','POST',p,status=409)[1]['status'],'STALE_RECONCILIATION')
    def test_comparison_income_edit_invalidates_old_source_review(self):
        b,data=self.imported();b.edit('PROPOSE',field='SALARY',text='1 lakh',unit='')
        self.assertEqual(b.call('/api/reconciliation',status=409)[1]['status'],'STALE_RECONCILIATION')
        b.call('/api/reconciliation/import','POST',self.statement_data(),headers=self.import_headers(b))
        self.assertEqual(b.call('/api/reconciliation')[1]['groups_reviewed'],0)
    def test_comparison_import_retry_does_not_erase_review_or_restore_batch(self):
        b=SessionClient();headers=self.import_headers(b,'import-stable');body=self.statement_data();b.call('/api/reconciliation/import','POST',body,headers=headers)
        data=b.call('/api/reconciliation')[1];b.call('/api/reconciliation/review','POST',self.source_choice(b,data));current=b.call('/api/reconciliation')[1]
        self.assertEqual(b.call('/api/reconciliation/import','POST',body,headers=headers)[1]['status'],'REPLAYED');self.assertEqual(b.call('/api/reconciliation')[1],current)
        self.assertEqual(b.call('/api/reconciliation/import','POST',body+b' ',headers=headers,status=409)[1]['status'],'IDEMPOTENCY_CONFLICT')
    def test_comparison_review_retry_keeps_latest_selection_and_amount_free_history(self):
        b,data=self.imported();first=self.source_choice(b,data,observation='savings_bank');b.call('/api/reconciliation/review','POST',first)
        latest=b.call('/api/reconciliation')[1];second=self.source_choice(b,latest,observation='savings_ais');b.call('/api/reconciliation/review','POST',second)
        current=b.call('/api/reconciliation')[1];self.assertEqual(b.call('/api/reconciliation/review','POST',first)[1]['status'],'REPLAYED');self.assertEqual(b.call('/api/reconciliation')[1],current)
        history=json.dumps(current['events'])
        for raw in ['5000.00','6500.00','BANK_A','REF_A']:self.assertNotIn(raw,history)
    def test_comparison_reopen_and_identical_import_preserve_explicit_intent(self):
        b,data=self.imported();b.call('/api/reconciliation/review','POST',self.source_choice(b,data));current=b.call('/api/reconciliation')[1]
        self.assertEqual(b.call('/api/reconciliation/import','POST',self.statement_data(),headers=self.import_headers(b))[1]['status'],'NO_CHANGE');self.assertEqual(b.call('/api/reconciliation')[1]['groups_reviewed'],1)
        g=next(x for x in current['groups'] if x['local_review']=='OBSERVATION_SELECTED');p={'operation':'REOPEN','request_id':str(uuid.uuid4()),'view_token':b.state['view_token'],'comparison_token':current['comparison_token'],'group_id':g['group_id']}
        b.call('/api/reconciliation/review','POST',p);self.assertEqual(b.call('/api/reconciliation')[1]['groups_reviewed'],0)
    def test_comparison_csv_headers_utf8_formulas_unknown_sources_reject_without_losing_batch(self):
        b,data=self.imported();body=self.statement_data()
        for bad in [b'wrong,header\n',b'\xff',body.replace(b'salary_manual,',b'=FORMULA,'),body.replace(b',BANK,',b',TRUST_ME,')]:
            b.call('/api/reconciliation/import','POST',bad,headers=self.import_headers(b),status=400)
        self.assertEqual(b.call('/api/reconciliation')[1],data)
    def test_comparison_import_body_and_types_bounded(self):
        b=SessionClient();b.call('/api/reconciliation/import','POST',b'x'*32769,headers=self.import_headers(b),status=413)
        h=self.import_headers(b);h['Content-Type']='application/json';b.call('/api/reconciliation/import','POST',b'{}',headers=h,status=415)
    def test_comparison_csrf_origin_and_authority_fields_cannot_upgrade_workflow(self):
        b,data=self.imported();h=self.import_headers(b);h['X-CSRF-Token']='wrong';b.call('/api/reconciliation/import','POST',self.statement_data(),headers=h,status=403)
        b.call('/api/reconciliation/review','POST',dict(self.source_choice(b,data),release_approved='true'),status=400)
        b.call('/api/reconciliation/review','POST',self.source_choice(b,data),headers={'Origin':'https://untrusted.invalid'},status=403)
    def test_comparison_import_view_and_request_id_are_required(self):
        b=SessionClient();h=self.import_headers(b);h.pop('X-Input-View');b.call('/api/reconciliation/import','POST',self.statement_data(),headers=h,status=400)
        h=self.import_headers(b);h['X-Input-View']='wrong';b.call('/api/reconciliation/import','POST',self.statement_data(),headers=h,status=409)
        h=self.import_headers(b);h.pop('X-Request-ID');b.call('/api/reconciliation/import','POST',self.statement_data(),headers=h,status=400)


    def source_ready(self,b=None,body=None,category='SALARY',observation=None):
        b=b or SessionClient()
        b.call('/api/reconciliation/import','POST',body or self.statement_data(),headers=self.import_headers(b))
        data=b.call('/api/reconciliation')[1];g=next(g for g in data['groups'] if g['category']==category)
        choice=self.source_choice(b,data,g,observation);b.call('/api/reconciliation/review','POST',choice)
        data=b.call('/api/reconciliation')[1];g=next(group for group in data['groups'] if group['group_id']==choice['group_id'])
        chosen=next(o for o in g['observations'] if o['observation_id']==choice['observation_id'])
        payload={'request_id':str(uuid.uuid4()),'view_token':b.state['view_token'],'comparison_token':data['comparison_token'],
                 'group_id':g['group_id'],'observation_id':chosen['observation_id'],'displayed_amount':chosen['gross_amount'],
                 'field':category,'whole_category_confirmed':'YES','replace_input_confirmed':'YES'}
        return b,payload
    @staticmethod
    def source_csv(*rows):
        header='observation_id,source,category,payer_alias,period,coverage,reference,gross_amount,tds_amount,value_kind'
        return (header+'\n'+'\n'.join(rows)+'\n').encode()
    @staticmethod
    def source_row(identifier='annual',coverage='ANNUAL',reference='TOTAL',gross='100.00',category='SALARY'):
        return ','.join([identifier,'BANK',category,'PAYER_A','FY2025-26',coverage,reference,gross,'10.00','REPORTED'])
    def test_source_proposal_replaces_confirmed_amount_resets_reviews_and_needs_confirmation(self):
        b=self.prepared('100000.00')
        for inventory in ['NEW_DEDUCTIONS','OLD_DEDUCTIONS']:b.edit('REVIEW',inventory=inventory)
        b,p=self.source_ready(b,observation='salary_form16');b.call('/api/reconciliation/propose','POST',p);b.refresh()
        self.assertEqual(b.input('SALARY'),{'field':'SALARY','state':'ESTIMATED','amount':'800000.00'})
        self.assertFalse(any(b.state['reviews'].values()));self.assertEqual(len(b.state['source_links']),1)
        self.assertEqual(b.state['source_links'][0]['current_fact_state'],'ESTIMATED');self.assertFalse(b.state['source_links'][0]['source_authenticity_verified'])
        self.assertEqual(b.call('/api/reconciliation',status=409)[1]['status'],'STALE_RECONCILIATION')
        b.edit('CONFIRM',field='SALARY',amount='800000.00');self.assertEqual(b.input('SALARY')['state'],'CONFIRMED')
        self.assertEqual(b.state['source_links'][0]['current_fact_state'],'CONFIRMED')
        result=b.call('/api/result')[1]['result'];self.assertEqual(result['status'],'BLOCKED');self.assertFalse(result['capabilities']['submit_return']);self.assertNotIn('annual_taxes',result)
        text=json.dumps(b.call('/api/history')[1])
        for secret in ['800000','40000','salary_form16','PAYER_A','FORM16']:self.assertNotIn(secret,text)
    def test_source_same_amount_still_demotes_confirmation(self):
        b,p=self.source_ready(self.prepared(),observation='salary_form16');b.call('/api/reconciliation/propose','POST',p);b.refresh()
        self.assertEqual(b.input('SALARY')['state'],'ESTIMATED');self.assertFalse(b.state['reviews']['INCOME'])
    def test_source_requires_exact_recorded_selection_and_current_comparison(self):
        b,data=self.imported();g=next(g for g in data['groups'] if g['category']=='SALARY')
        p={'request_id':str(uuid.uuid4()),'view_token':b.state['view_token'],'comparison_token':data['comparison_token'],'group_id':g['group_id'],'observation_id':'salary_form16','displayed_amount':'800000.00','field':'SALARY','whole_category_confirmed':'YES','replace_input_confirmed':'YES'}
        self.assertEqual(b.call('/api/reconciliation/propose','POST',p,status=409)[1]['status'],'SOURCE_SELECTION_REQUIRED')
        b.call('/api/reconciliation/review','POST',self.source_choice(b,data,g,'salary_form16'))
        self.assertEqual(b.call('/api/reconciliation/propose','POST',p,status=409)[1]['status'],'STALE_RECONCILIATION')
        p['comparison_token']=b.call('/api/reconciliation')[1]['comparison_token'];p['observation_id']='salary_manual'
        self.assertEqual(b.call('/api/reconciliation/propose','POST',p,status=409)[1]['status'],'SOURCE_SELECTION_REQUIRED')
        self.assertEqual(b.refresh()['source_links'],[])
    def test_source_blocks_conflicts_missing_and_repeated_gross_after_selection(self):
        for category,observation in [('SAVINGS_INTEREST','savings_bank'),('DEPOSIT_INTEREST','deposit_bank')]:
            b,p=self.source_ready(category=category,observation=observation)
            self.assertEqual(b.call('/api/reconciliation/propose','POST',p,status=409)[1]['status'],'GROSS_SOURCE_REVIEW_REQUIRED')
            self.assertEqual(b.refresh()['source_links'],[]);self.assertEqual(b.input(category)['state'],'UNKNOWN')
    def test_source_blocks_transaction_and_overlapping_category_groups(self):
        for rows,code in [([self.source_row(coverage='TRANSACTION')],'ANNUAL_SOURCE_REQUIRED'),
                          ([self.source_row(),self.source_row('tx','TRANSACTION','TX1')],'CATEGORY_COVERAGE_AMBIGUOUS'),
                          ([self.source_row(),self.source_row('other','ANNUAL','OTHER')],'CATEGORY_COVERAGE_AMBIGUOUS')]:
            b,p=self.source_ready(body=self.source_csv(*rows))
            self.assertEqual(b.call('/api/reconciliation/propose','POST',p,status=409)[1]['status'],code)
            self.assertEqual(b.refresh()['source_links'],[])
    def test_source_requires_both_acknowledgements_and_exact_gross_destination(self):
        b,p=self.source_ready(observation='salary_form16')
        for fields,code in [({'whole_category_confirmed':'NO'},'WHOLE_CATEGORY_REVIEW_REQUIRED'),({'replace_input_confirmed':'NO'},'REPLACEMENT_REVIEW_REQUIRED'),
                            ({'displayed_amount':'40000.00'},'SOURCE_AMOUNT_CHANGED'),({'displayed_amount':'760000.00'},'SOURCE_AMOUNT_CHANGED'),({'field':'OTHER_INTEREST'},'SOURCE_FIELD_MISMATCH')]:
            self.assertEqual(b.call('/api/reconciliation/propose','POST',dict(p,**fields),status=409)[1]['status'],code)
        b.call('/api/reconciliation/propose','POST',dict(p,whole_category_confirmed='APPROVED'),status=400)
        self.assertEqual(b.call('/api/history')[1]['events'],[])
    def test_source_retry_after_later_edits_and_import_does_not_restore_amount_or_lineage(self):
        b,p=self.source_ready();b.call('/api/reconciliation/propose','POST',p);b.refresh()
        b.edit('PROPOSE',field='SALARY',text='900000 rupees',unit='');self.assertEqual(b.state['source_links'],[])
        b.call('/api/reconciliation/import','POST',self.statement_data(),headers=self.import_headers(b));comparison=b.call('/api/reconciliation')[1]
        current=b.refresh();history=b.call('/api/history')[1]
        receipt=b.call('/api/reconciliation/propose','POST',p)[1];self.assertEqual(receipt['status'],'REPLAYED');self.assertFalse(receipt['state_changed'])
        self.assertEqual(b.refresh(),current);self.assertEqual(b.call('/api/reconciliation')[1],comparison);self.assertEqual(b.call('/api/history')[1],history)
    def test_source_retry_after_confirmation_does_not_demote_or_relink(self):
        b,p=self.source_ready();b.call('/api/reconciliation/propose','POST',p);b.refresh();b.edit('CONFIRM',field='SALARY',amount='800000.00');current=b.state
        self.assertEqual(b.call('/api/reconciliation/propose','POST',p)[1]['status'],'REPLAYED');self.assertEqual(b.refresh(),current)
    def test_source_changed_retry_body_cannot_reuse_request_id(self):
        b,p=self.source_ready();b.call('/api/reconciliation/propose','POST',p)
        for fields in [{'displayed_amount':'900000.00'},{'comparison_token':'different'},{'whole_category_confirmed':'NO'},{'observation_id':'salary_manual' if p['observation_id']=='salary_form16' else 'salary_form16'}]:
            self.assertEqual(b.call('/api/reconciliation/propose','POST',dict(p,**fields),status=409)[1]['status'],'IDEMPOTENCY_CONFLICT')
    def test_source_and_manual_income_commands_share_request_id_namespace(self):
        b,p=self.source_ready();manual=b.payload('PROPOSE',field='OTHER_INTEREST',text='0 rupees',unit='');b.call('/api/edit','POST',manual);b.refresh()
        self.assertEqual(b.call('/api/reconciliation/propose','POST',dict(p,request_id=manual['request_id']),status=409)[1]['status'],'IDEMPOTENCY_CONFLICT')
        b,p=self.source_ready(b);b.call('/api/reconciliation/propose','POST',p)
        manual=b.payload('CLEAR',field='SALARY');manual['request_id']=p['request_id']
        self.assertEqual(b.call('/api/edit','POST',manual,status=409)[1]['status'],'IDEMPOTENCY_CONFLICT')
    def test_source_link_is_cleared_by_manual_same_value_and_clear(self):
        b,p=self.source_ready();b.call('/api/reconciliation/propose','POST',p);b.refresh()
        self.assertEqual(b.edit('PROPOSE',field='SALARY',text='800000.00',unit='RUPEES')['status'],'NO_CHANGE');self.assertEqual(b.state['source_links'],[])
        current=b.state;self.assertEqual(b.call('/api/reconciliation/propose','POST',p)[1]['status'],'REPLAYED');self.assertEqual(b.refresh(),current)
        b,p=self.source_ready(b);b.call('/api/reconciliation/propose','POST',p);b.refresh();b.edit('CLEAR',field='SALARY')
        self.assertEqual(b.input('SALARY')['state'],'UNKNOWN');self.assertEqual(b.state['source_links'],[])
    def test_old_manual_retry_does_not_clear_new_source_link(self):
        b=SessionClient();manual=b.payload('PROPOSE',field='SALARY',text='1 lakh',unit='');b.call('/api/edit','POST',manual);b.refresh()
        b,p=self.source_ready(b);b.call('/api/reconciliation/propose','POST',p);current=b.refresh()
        self.assertEqual(b.call('/api/edit','POST',manual)[1]['status'],'REPLAYED');self.assertEqual(b.refresh(),current)
    def test_source_exact_zero_is_a_proposal_and_unknown_is_not_zero(self):
        b,p=self.source_ready(body=self.source_csv(self.source_row(gross='0.00')));b.call('/api/reconciliation/propose','POST',p);b.refresh()
        self.assertEqual(b.input('SALARY'),{'field':'SALARY','state':'ESTIMATED','amount':'0.00'});self.assertEqual(len(b.state['source_links']),1)
        b=SessionClient();b.call('/api/reconciliation/import','POST',self.source_csv(self.source_row(gross='UNKNOWN')),headers=self.import_headers(b));data=b.call('/api/reconciliation')[1]
        self.assertEqual(b.call('/api/reconciliation/review','POST',self.source_choice(b,data,data['groups'][0]),status=409)[1]['status'],'GROSS_VALUE_MISSING')
        self.assertEqual(b.refresh()['source_links'],[])
    def test_source_endpoint_enforces_authentication_csrf_origin_view_and_closed_session(self):
        b,p=self.source_ready();SessionClient(False).call('/api/reconciliation/propose','POST',p,status=401)
        b.call('/api/reconciliation/propose','POST',p,headers={'X-CSRF-Token':'wrong'},status=403)
        b.call('/api/reconciliation/propose','POST',p,headers={'Origin':None},status=403)
        other=SessionClient();other.call('/api/reconciliation/propose','POST',p,status=409)
        b.edit('PROPOSE',field='OTHER_INTEREST',text='0 rupees',unit='');self.assertEqual(b.call('/api/reconciliation/propose','POST',p,status=409)[1]['status'],'STALE_INPUT')
        b.call('/api/close','POST',b'');b.call('/api/reconciliation/propose','POST',p,status=401)
    def test_source_endpoint_rejects_extra_fields_duplicates_invalid_utf8_and_size(self):
        b,p=self.source_ready();raw=urllib.parse.urlencode(p).encode();h={'Content-Type':'application/x-www-form-urlencoded'}
        b.call('/api/reconciliation/propose',status=405);b.call('/api/reconciliation/propose','POST',b'{}',headers={'Content-Type':'application/json'},status=415)
        for body in [raw+b'&field=OTHER_INTEREST',b'x=%FF',b'x=%GG',b'x'*4097]:b.call('/api/reconciliation/propose','POST',body,headers=h,status=413 if len(body)>4096 else 400)
        for field in ['rule_state','reference_state','source_authenticity_verified','confirmed','amount','actor_id']:
            self.assertEqual(b.call('/api/reconciliation/propose','POST',dict(p,**{field:'APPROVED'}),status=400)[1]['status'],'UNEXPECTED_FIELDS')
        self.assertEqual(b.call('/api/history')[1]['events'],[])
    def test_concurrent_source_retries_apply_one_income_edit(self):
        b,p=self.source_ready();cookie='; '.join(x.name+'='+x.value for x in b.cookies)
        def send():return SessionClient(False).call('/api/reconciliation/propose','POST',p,headers={'Cookie':cookie,'Origin':BASE,'X-CSRF-Token':b.state['csrf_token']})[1]['status']
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:self.assertEqual(sorted(pool.map(lambda _:send(),range(2))),['APPLIED','REPLAYED'])
        self.assertEqual(len(b.call('/api/history')[1]['events']),1);self.assertEqual(len(b.refresh()['source_links']),1)
    def test_source_new_commands_cannot_both_replace_the_same_input_version(self):
        b,p=self.source_ready();other=dict(p,request_id=str(uuid.uuid4()));cookie='; '.join(x.name+'='+x.value for x in b.cookies)
        def send(payload):
            client=SessionClient(False);request=urllib.request.Request(BASE+'/api/reconciliation/propose',data=urllib.parse.urlencode(payload).encode(),headers={'Cookie':cookie,'Origin':BASE,'X-CSRF-Token':b.state['csrf_token'],'Content-Type':'application/x-www-form-urlencoded'},method='POST')
            try:response=client.opener.open(request,timeout=5)
            except urllib.error.HTTPError as error:response=error
            with response:return json.loads(response.read())['status']
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:self.assertEqual(sorted(pool.map(send,[p,other])),['APPLIED','STALE_INPUT'])
        self.assertEqual(len(b.call('/api/history')[1]['events']),1);self.assertEqual(len(b.refresh()['source_links']),1)
    def test_source_proposal_uses_the_existing_bounded_income_edit_budget(self):
        b,p=self.source_ready()
        for _ in range(99):b.call('/api/edit','POST',b.payload('CLEAR',field='OTHER_INTEREST'))
        self.assertEqual(b.call('/api/reconciliation/propose','POST',p)[1]['status'],'APPLIED');b.refresh()
        self.assertEqual(b.edit('CLEAR',expected_status=409,field='SALARY')['status'],'SESSION_LIMIT_REACHED')
        self.assertEqual(b.call('/api/reconciliation/propose','POST',p)[1]['status'],'REPLAYED')
        self.assertEqual(len(b.refresh()['source_links']),1)
    def test_source_link_is_historical_unverified_and_excludes_amounts_and_bindings(self):
        b,p=self.source_ready(observation='salary_form16');b.call('/api/reconciliation/propose','POST',p);links=b.refresh()['source_links'];text=json.dumps(links)
        self.assertEqual(links[0]['source_status'],'HISTORICAL_UNVERIFIED_OBSERVATION');self.assertEqual(links[0]['observation_id'],'salary_form16')
        for secret in ['800000','40000','valueTag','fingerprint','body','snapshot','EMP_A','SALARY_TOTAL']:self.assertNotIn(secret,text)

def main():
    global BASE
    jar=ROOT/'build/tax-preparation-local-demo-0.4.0.jar'
    if not jar.exists():subprocess.run([sys.executable,str(ROOT/'build_app.py')],check=True)
    process=subprocess.Popen(['java','--add-modules','jdk.httpserver','-jar',str(jar),'--port','0'],cwd=ROOT,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    startup=queue.Queue()
    def read_start():
        for line in process.stdout:
            if line.startswith('LOCAL_DEMO_URL='):startup.put(line.strip().split('=',1)[1]);break
    threading.Thread(target=read_start,daemon=True).start()
    try:
        BASE=startup.get(timeout=15)
        result=unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(HTTPTests))
        report={'application':'local-synthetic-preparation','version':'0.4.0','http_scenario_groups':result.testsRun,
                'failures':len(result.failures),'errors':len(result.errors),'jar_sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),
                'core_jar_sha256':hashlib.sha256((ROOT/'build/lib/tax-result-safety-0.1.4.jar').read_bytes()).hexdigest(),
                'engine_jar_sha256':hashlib.sha256((ROOT/'build/lib/tax-planner-workbench-0.1.0.jar').read_bytes()).hexdigest(),
                'uses_real_loopback_http':True,
                'browser_ui_tested':False,'tax_correctness_validated':False,'production_ready':False}
        (ROOT/'build/http_test_results.json').write_text(json.dumps(report,indent=2)+'\n')
        print(json.dumps(report,indent=2))
        return 0 if result.wasSuccessful() else 1
    finally:
        process.terminate()
        try:process.wait(timeout=5)
        except subprocess.TimeoutExpired:process.kill();process.wait(timeout=5)

if __name__=='__main__':raise SystemExit(main())
