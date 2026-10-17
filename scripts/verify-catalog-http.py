from html.parser import HTMLParser
from urllib.parse import urljoin, urlparse, parse_qs, urlencode
from urllib.request import Request, urlopen
import json

class Catalog(HTMLParser):
    def __init__(self):super().__init__();self.ids=[];self.links={};self.link=None;self.counting=False;self.total=''
    def handle_starttag(self,tag,attrs):
        attrs=dict(attrs);classes=attrs.get('class','').split()
        if tag=='a' and 'card-img-link' in classes:self.ids.append(attrs['href'])
        if tag=='p' and 'simple-catalog-count' in classes:self.counting=True
        if tag=='a':self.link=attrs.get('href')
    def handle_data(self,data):
        if self.counting:self.total+=data
        if self.link and data.strip() in ['Next','Previous']:self.links[data.strip()]=self.link
    def handle_endtag(self,tag):
        if tag=='p':self.counting=False
        if tag=='a':self.link=None

def read(route):
    with urlopen(Request(urljoin('https://nuobo.store',route),headers={'User-Agent':'Mozilla/5.0'}),timeout=20) as response:
        parser=Catalog();parser.feed(response.read().decode());return parser

report=[]
for route in ['/products','/category/christmas','/category/halloween','/search?q=inflatable']:
    first=read(route);second=read(first.links['Next'])
    assert len(first.ids)==len(second.ids)==12
    assert not set(first.ids)&set(second.ids)
    assert read(second.links['Previous']).ids==first.ids
    total=int(first.total.split()[0]);parts=urlparse(route);params=parse_qs(parts.query)
    params['page']=[str((total-1)//12)]
    last=read(parts.path+'?'+urlencode(params,doseq=True))
    assert len(last.ids)==(total%12 or 12) and 'Next' not in last.links
    if parts.path=='/search':assert parse_qs(urlparse(first.links['Next']).query)['q']==['inflatable']
    report.append({'route':route,'first':12,'second':12,'total':total,'last':len(last.ids),'unique':True})
print(json.dumps(report))
