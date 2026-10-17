from urllib.request import Request, build_opener, HTTPRedirectHandler, HTTPCookieProcessor
from urllib.error import HTTPError
from urllib.parse import urlencode
from http.cookiejar import CookieJar
from html.parser import HTMLParser
import json

class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, *args): return None
opener = build_opener(HTTPCookieProcessor(CookieJar()), NoRedirect)
class Form(HTMLParser):
    def __init__(self): super().__init__(); self.token = ''
    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == 'meta' and attrs.get('name') == '_csrf': self.token = attrs['content']
def request(path, fields=None, ajax=False):
    headers = {'User-Agent':'Mozilla/5.0'}
    if ajax: headers['X-Requested-With'] = 'XMLHttpRequest'
    try:
        response = opener.open(Request('https://nuobo.store'+path, data=urlencode(fields).encode() if fields else None, headers=headers),timeout=20)
    except HTTPError as error: response = error
    with response: return response.code, response.read().decode()
code, body = request('/cart')
form = Form(); form.feed(body)
assert form.token
code, _ = request('/cart/add', {'productId':355,'quantity':1,'_csrf':form.token})
assert code == 302
code, body = request('/cart/update', {'productId':355,'quantity':2,'_csrf':form.token}, True)
data = json.loads(body)
assert code == 200 and data['quantity'] == 2 and not data['removed']
code, _ = request('/cart/add', {'productId':9223372036854775807,'quantity':1,'_csrf':form.token})
assert code == 404
code, body = request('/cart/update', {'productId':355,'quantity':0,'_csrf':form.token}, True)
assert code == 200 and json.loads(body)['removed']
print(json.dumps({'ajaxStoredQuantity':2,'ajaxStock':data['stock'],'invalidProductStatus':404,'zeroQuantityRemoved':True}))
