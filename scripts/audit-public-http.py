from html.parser import HTMLParser
from urllib.request import Request, build_opener, HTTPRedirectHandler
from urllib.error import HTTPError
from pathlib import Path
import json
import xml.etree.ElementTree as ET

class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, *args): return None
opener = build_opener(NoRedirect)
class Metadata(HTMLParser):
    def __init__(self):
        super().__init__(); self.title = ''; self.in_title = False; self.h1 = 0; self.meta = {}; self.canonical = ''; self.policy = False; self.policy_text = ''
    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == 'title': self.in_title = True
        if tag == 'h1': self.h1 += 1
        if tag == 'meta': self.meta[attrs.get('name', attrs.get('property'))] = attrs.get('content', '')
        if tag == 'link' and attrs.get('rel') == 'canonical': self.canonical = attrs.get('href', '')
        if tag == 'article' and 'policy-content' in attrs.get('class', ''): self.policy = True
    def handle_endtag(self, tag):
        if tag == 'title': self.in_title = False
        if tag == 'article': self.policy = False
    def handle_data(self, data):
        if self.in_title: self.title += data
        if self.policy: self.policy_text += data.strip() + ' '
def read(route, data=None):
    try:
        response = opener.open(Request('https://nuobo.store' + route, data=data, headers={'User-Agent':'Mozilla/5.0'}), timeout=20)
    except HTTPError as error: response = error
    with response: return response.code, dict(response.headers), response.read().decode()

report = {'pages': [], 'boundaries': []}
for route in ['/', '/products', '/category/christmas', '/category/halloween', '/product/355', '/about', '/contact', '/faq', '/privacy', '/return-policy', '/login', '/register', '/inquiry', '/cart']:
    code, headers, body = read(route)
    parser = Metadata(); parser.feed(body)
    assert code == 200 and parser.title and parser.meta.get('description'), route
    report['pages'].append({'route':route, 'status':code, 'title':parser.title, 'description':parser.meta.get('description'), 'canonical':parser.canonical, 'h1':parser.h1, 'robots':parser.meta.get('robots'), 'policyCharacters':len(parser.policy_text.strip())})
for route, expected in [('/category/not-a-real-category',404),('/product/9223372036854775807',404),('/admin/dashboard',302),('/account',302),('/checkout',302)]:
    code, headers, body = read(route)
    assert code == expected, (route, code)
    report['boundaries'].append({'route':route, 'status':code, 'redirectScheme':headers.get('Location','').split(':')[0] if headers.get('Location','').startswith('http') else ''})
code, headers, body = read('/checkout/notify', b'trade_status=INVALID')
assert code == 200 and body.strip() == 'failure', (code, body[:40])
report['unsignedPaymentCallbackRejected'] = True
code, headers, body = read('/sitemap.xml')
assert code == 200
tree = ET.fromstring(body)
locations = [item.text for item in tree.findall('.//{http://www.sitemaps.org/schemas/sitemap/0.9}loc')]
assert len(locations) == len(set(locations))
report['sitemapUrls'] = len(locations)
code, headers, body = read('/robots.txt')
report['robots'] = {'status':code, 'body':body}
out = Path(__file__).resolve().parents[1] / 'output/final-audit-http.json'
out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps(report, ensure_ascii=False))
