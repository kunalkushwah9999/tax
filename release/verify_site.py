#!/usr/bin/env python3
"""Bounded public HTTPS deployment probes. No customer creation, payments or filing."""
from pathlib import Path
import argparse, hashlib, http.client, json, re, secrets, ssl, sys
from urllib.parse import urlsplit

def origin(value, fixture=False):
    u=urlsplit(value)
    if u.scheme!='https' or not u.hostname or u.username or u.password or u.path or u.query or u.fragment or any(ord(c)<33 for c in value):
        raise ValueError('Use an HTTPS origin only, without credentials, path, query or fragment')
    port=u.port or 443
    if not fixture and (port!=443 or u.hostname in ['localhost','127.0.0.1','::1']):
        raise ValueError('Public verification requires HTTPS port 443; loopback is test-fixture-only')
    if not 1<=port<=65535:raise ValueError('Invalid HTTPS port')
    canonical='https://'+u.hostname+(':'+str(port) if port!=443 else '')
    if canonical!=value:raise ValueError('Use the canonical lowercase HTTPS origin')
    return u.hostname,port

class Client:
    def __init__(self,base,*,ca_file=None,fixture=False):
        if ca_file and not fixture:raise ValueError('Custom CA files are restricted to explicitly labelled fixture probes')
        self.host,self.port=origin(base,fixture);self.base=base
        self.context=ssl.create_default_context(cafile=str(ca_file) if ca_file else None)
        self.context.minimum_version=ssl.TLSVersion.TLSv1_2
        self.requests=[];self.peer=None
    def request(self,path,method='GET',payload=None,headers=None,limit=262144):
        if not path.startswith('/') or '?' in path or '\\' in path:raise ValueError('Invalid probe path')
        h={'Accept':'application/json','Accept-Encoding':'identity'}
        if headers:h.update(headers)
        body=None if payload is None else json.dumps(payload).encode()
        if body is not None:h['Content-Type']='application/json'
        c=http.client.HTTPSConnection(self.host,self.port,context=self.context,timeout=8)
        try:
            c.connect();self.peer={'certificate_sha256':hashlib.sha256(c.sock.getpeercert(binary_form=True)).hexdigest(),'tls_version':c.sock.version(),'certificate_expires':c.sock.getpeercert().get('notAfter'),'trust_verified':True}
            c.request(method,path,body=body,headers=h);r=c.getresponse();data=r.read(limit+1)
            if len(data)>limit:raise ValueError('Probe response exceeded bound')
            self.requests.append({'path':path,'method':method,'status':r.status,'bytes':len(data)})
            return r.status,dict((k.lower(),v) for k,v in r.getheaders()),data
        finally:c.close()

def verify(base,*,ca_file=None,fixture=False,application_id=None,fingerprint=None):
    if bool(application_id)!=bool(fingerprint):raise ValueError('Application ID and Play app-signing SHA-256 are required together')
    if fingerprint and not re.fullmatch(r'(?:[0-9a-fA-F]{2}:){31}[0-9a-fA-F]{2}',fingerprint):raise ValueError('Use the full colon-separated app-signing SHA-256 fingerprint')
    client=Client(base,ca_file=ca_file,fixture=fixture);checks=[]
    def check(name,value):checks.append({'check':name,'passed':bool(value)})
    def get_json(path,method='GET',payload=None,headers=None):
        status,h,raw=client.request(path,method,payload,headers,65536)
        try:data=json.loads(raw)
        except (ValueError,UnicodeDecodeError):data=None
        return status,h,data
    status,h,data=get_json('/api/health');check('health_is_current_synthetic_app',status==200 and isinstance(data,dict) and data.get('version')=='5.1.0' and data.get('status')=='OK' and data.get('production_ready') is False)
    status,h,data=get_json('/api/bootstrap');cookie=h.get('set-cookie','');csrf=data.get('csrf_token','') if isinstance(data,dict) else ''
    if not isinstance(csrf,str):csrf=''
    check('anonymous_bootstrap',status==200 and isinstance(data,dict) and data.get('user','missing') is None and re.fullmatch(r'[A-Za-z0-9_-]{43}',csrf))
    check('secure_host_cookie',cookie.startswith('__Host-tax_startup_session=') and '; Secure' in cookie and '; HttpOnly' in cookie and '; SameSite=Strict' in cookie and '; Path=/' in cookie and 'domain=' not in cookie.lower())
    check('api_no_store_and_hsts','no-store' in h.get('cache-control','').lower() and 'max-age=' in h.get('strict-transport-security','').lower())
    auth={'Cookie':cookie.split(';',1)[0],'Origin':base}
    status,_,_=get_json('/api/cases',headers=auth);check('private_case_api_rejects_anonymous',status==401)
    status,_,_=get_json('/api/health',headers={'Origin':'https://cross-origin.invalid'});check('foreign_origin_rejected',status==403)
    status,_,_=get_json('/api/health',headers={'Host':'wrong-host.invalid'});check('foreign_host_rejected',status==421)
    payload={'receipt_token':secrets.token_urlsafe(32)}
    status,_,_=get_json('/api/account-deletion/status','POST',payload,{**auth,'X-CSRF-Token':'wrong'});check('receipt_csrf_enforced',status==403)
    if re.fullmatch(r'[A-Za-z0-9_-]{43}',csrf):
        status,_,_=get_json('/api/account-deletion/status','POST',payload,{**auth,'X-CSRF-Token':csrf});check('unknown_receipt_not_reported_complete',status==404)
    else:check('unknown_receipt_not_reported_complete',False)
    status,h,raw=client.request('/');csp=h.get('content-security-policy','')
    check('public_page_and_install_manifest',status==200 and b'/manifest.webmanifest' in raw and 'text/html' in h.get('content-type',''))
    check('web_security_headers',"frame-ancestors 'none'" in csp and "worker-src 'self'" in csp and "manifest-src 'self'" in csp and h.get('x-content-type-options')=='nosniff' and h.get('x-frame-options')=='DENY' and h.get('referrer-policy')=='no-referrer')
    status,h,data=get_json('/manifest.webmanifest');check('installable_manifest',status==200 and isinstance(data,dict) and data.get('start_url')=='/' and data.get('scope')=='/' and data.get('display')=='standalone' and isinstance(data.get('icons'),list) and len(data['icons'])>=2)
    status,h,raw=client.request('/sw.js');check('worker_served_without_http_cache',status==200 and 'no-store' in h.get('cache-control','').lower() and ('javascript' in h.get('content-type','')))
    status,h,raw=client.request('/account-deletion');check('public_deletion_page_is_html',status==200 and 'text/html' in h.get('content-type','') and b'deletion-status-form' in raw and b'TaxDesk' in raw)
    status,h,data=get_json('/.well-known/assetlinks.json');check('asset_links_are_public_json',status==200 and 'application/json' in h.get('content-type','') and isinstance(data,list))
    if fingerprint:
        matched=isinstance(data,list) and any(isinstance(r,dict) and isinstance(r.get('relation'),list) and 'delegate_permission/common.handle_all_urls' in r['relation'] and isinstance(r.get('target'),dict) and r['target'].get('namespace')=='android_app' and r['target'].get('package_name')==application_id and isinstance(r['target'].get('sha256_cert_fingerprints'),list) and fingerprint.upper() in r['target']['sha256_cert_fingerprints'] for r in data)
        check('play_app_signing_identity_matches',matched)
    return {'origin':base,'fixture':fixture,'public_endpoint_smoke_passed':False if fixture else all(c['passed'] for c in checks),'cloud_release_validated':False,
            'infrastructure_smoke_passed':all(c['passed'] for c in checks),'checks':checks,'requests':client.requests,'peer':client.peer,
            'android_identity_checked':bool(fingerprint),'production_ready':False,'independent_tax_approvals':0,
            'limitations':['No authenticated customer flow or real Nginx process is certified by these probes','No scale, backup restore, statutory tax, live provider or Android device approval']}

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--origin',required=True);p.add_argument('--ca-file',type=Path);p.add_argument('--fixture',action='store_true');p.add_argument('--application-id');p.add_argument('--app-signing-sha256');p.add_argument('--out',type=Path,required=True);a=p.parse_args()
    if a.out.exists():p.error('Output must be a new file')
    try:result=verify(a.origin,ca_file=a.ca_file,fixture=a.fixture,application_id=a.application_id,fingerprint=a.app_signing_sha256)
    except (ValueError,OSError,http.client.HTTPException) as error:result={'infrastructure_smoke_passed':False,'production_ready':False,'error_type':type(error).__name__,'reason':'HTTPS probe could not be completed; inspect local trust/network/configuration.'}
    with a.out.open('x',encoding='utf-8') as f:json.dump(result,f,indent=2);f.write('\n')
    print(json.dumps({'report':str(a.out),'infrastructure_smoke_passed':result['infrastructure_smoke_passed'],'production_ready':False}))
    return 0 if result['infrastructure_smoke_passed'] else 2
if __name__=='__main__':sys.exit(main())
