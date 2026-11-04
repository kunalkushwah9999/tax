#!/usr/bin/env python3
"""Real TLS + Chromium + Java checks for the generated layer. Not an Nginx/Android test."""
from pathlib import Path
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import argparse, hashlib, http.client, http.cookiejar, json, mimetypes, queue, socket, ssl, subprocess, sys, tempfile, threading, urllib.request
import prepare

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--chromium', type=Path, required=True)
    p.add_argument('--playwright', type=Path, required=True)
    p.add_argument('--output-dir', type=Path, required=True)
    args = p.parse_args(); args.output_dir.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='taxdesk-release-browser-') as temp:
        folder = Path(temp); web = folder / 'bundle'
        prepare.render(prepare.configuration('https://pilot.taxdesk.test', 'in.taxdesk.pilot', 'support@taxdesk.test', []), web)
        with socket.socket() as sock:
            sock.bind(('127.0.0.1', 0)); backend_port = sock.getsockname()[1]
        class Proxy(BaseHTTPRequestHandler):
            def log_message(self, *args): pass
            def do_GET(self): self.handle_request()
            def do_POST(self): self.handle_request()
            def handle_request(self):
                if self.path.startswith('/api/'):
                    connection = http.client.HTTPConnection('127.0.0.1', backend_port, timeout=15)
                    try:
                        connection.request(self.command, self.path, body=self.rfile.read(int(self.headers.get('Content-Length', '0'))) or None,
                            headers={k: v for k, v in self.headers.items() if k.lower() not in ['connection', 'transfer-encoding']})
                        response = connection.getresponse(); data = response.read(); self.send_response(response.status)
                        for k, v in response.getheaders():
                            if k.lower() not in ['connection', 'transfer-encoding', 'content-length', 'server', 'date']: self.send_header(k, v)
                    finally: connection.close()
                else:
                    name = self.path.split('?', 1)[0]
                    if name == '/': name = '/index.html'
                    if name == '/account-deletion': name = '/account-deletion.html'
                    path = web / 'web' / name.lstrip('/')
                    if '..' in name or not path.is_file(): self.send_error(404); return
                    data = path.read_bytes(); self.send_response(200)
                    content_type = 'application/manifest+json' if name.endswith('.webmanifest') else mimetypes.guess_type(name)[0] or 'application/octet-stream'
                    self.send_header('Content-Type', content_type)
                    self.send_header('Cache-Control', 'no-store')
                    self.send_header('Content-Security-Policy', "default-src 'none'; script-src 'self'; worker-src 'self'; manifest-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'; object-src 'none'")
                self.send_header('Content-Length', str(len(data))); self.end_headers(); self.wfile.write(data)
        proxy = ThreadingHTTPServer(('127.0.0.1', 0), Proxy)
        base = 'https://localhost:' + str(proxy.server_port)
        cert, key = folder / 'cert.pem', folder / 'key.pem'
        subprocess.run(['openssl', 'req', '-x509', '-newkey', 'rsa:2048', '-nodes', '-days', '1', '-keyout', str(key), '-out', str(cert), '-subj', '/CN=localhost', '-addext', 'subjectAltName=DNS:localhost'], check=True, capture_output=True)
        tls = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER); tls.minimum_version = ssl.TLSVersion.TLSv1_2
        tls.load_cert_chain(cert, key); proxy.socket = tls.wrap_socket(proxy.socket, server_side=True)
        app = subprocess.Popen([sys.executable, str(prepare.ROOT / 'startup_app/run.py'), '--port', str(backend_port), '--data-dir', str(folder / 'records'), '--public-origin', base], stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
        ready = queue.Queue()
        def read():
            for line in app.stdout:
                if line.startswith('STARTUP_URL='): ready.put(True)
        threading.Thread(target=read, daemon=True).start()
        try:
            ready.get(timeout=20); threading.Thread(target=proxy.serve_forever, daemon=True).start()
            cookies = http.cookiejar.CookieJar()
            opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies), urllib.request.HTTPSHandler(context=ssl.create_default_context(cafile=str(cert))))
            with opener.open(base + '/api/bootstrap') as response: csrf = json.load(response)['csrf_token']
            password = 'Synthetic-release-fixture-2026!'
            request = urllib.request.Request(base + '/api/register', data=json.dumps({'username': 'release_owner', 'display_name': 'Synthetic release owner', 'password': password}).encode(), headers={'Content-Type': 'application/json', 'Origin': base, 'X-CSRF-Token': csrf}, method='POST')
            with opener.open(request) as response: assert json.load(response)['user']
            config = {'base': base, 'password': password, 'chromium': str(args.chromium.absolute()), 'playwright': str(args.playwright.absolute()), 'output': str(args.output_dir.absolute())}
            path = folder / 'browser.json'; path.write_text(json.dumps(config)); path.chmod(0o600)
            result = subprocess.run(['node', str(prepare.HERE / 'browser_checks.mjs'), str(path)], capture_output=True, text=True, timeout=120)
            if result.returncode:
                print(result.stdout[-2000:]); print(result.stderr[-2000:]); return result.returncode
            report = json.loads((args.output_dir / 'browser-result.json').read_text())
            report.update({'runtime_jar_sha256': hashlib.sha256((prepare.ROOT / 'startup_app/build' / prepare.JAR).read_bytes()).hexdigest(),
                'test_fixture': 'disposable TLS static proxy plus actual Java local storage', 'nginx_runtime_tested': False,
                'android_runtime_tested': False, 'cloud_deployed': False, 'production_ready': False,
                'sources': {str(p.relative_to(prepare.ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(prepare.HERE.rglob('*')) if p.is_file() and '__pycache__' not in p.parts and p.suffix in ['.py', '.mjs', '.js', '.css', '.html', '.xml', '.gradle', '.conf']}})
            (args.output_dir / 'browser-result.json').write_text(json.dumps(report, indent=2) + '\n')
            print(json.dumps({k: report[k] for k in ['browser_checks_passed', 'js_errors', 'runtime_jar_sha256', 'nginx_runtime_tested', 'android_runtime_tested', 'production_ready']}, indent=2))
            return 0
        finally:
            app.terminate()
            try: app.wait(timeout=10)
            except subprocess.TimeoutExpired: app.kill(); app.wait(timeout=5)
            proxy.shutdown(); proxy.server_close()

if __name__ == '__main__': sys.exit(main())
