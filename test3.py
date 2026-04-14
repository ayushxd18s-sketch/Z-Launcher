import json
import urllib.request

try:
    print(urllib.request.urlopen("https://etnode.zkitefly.eu.org/node1").read().decode("utf-8"))
except Exception as e:
    print("Error:", e)
