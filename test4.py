import urllib.request
req = urllib.request.Request("https://etnode.zkitefly.eu.org/node1", headers={'User-Agent': 'Mozilla/5.0'})
try:
    print(urllib.request.urlopen(req).read().decode("utf-8"))
except Exception as e:
    print("Error:", e)
