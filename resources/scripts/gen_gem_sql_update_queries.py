"""
A python script that maps gems to their toolbar icons
"""

from urllib.request import urlopen
from json import load
from sys import stdout

def printf(format, *args):
  stdout.write(format % args)
  
def getGemList(url):
  data = loads(urlopen(url).read().decode("utf-8"))
  gemList = []
  
  for item in data:
    if item["group"] == "support":
      continue
    if item["name"] not in gemList:
      gemList.append(item["name"])
  
  return gemList

def genGemMap(gemList):
  gemMap = {}
  count = 0
  
  for gem in gemList:
    count += 1
    url = "https://poedb.tw/us/hover.php?t=gem&n=" + gem.replace(" ", "+")
    htmlReply = urlopen(url).read().decode("utf-8")
    
    # Find indexes of urls in html reply
    URL1_lower = htmlReply.find("https://")
    URL1_upper = htmlReply.find("\"", URL1_lower)
    URL2_lower = htmlReply.find("https://", URL1_upper)
    URL2_upper = htmlReply.find("\"", URL2_lower)
    
    # Extract urls from reply
    gemMap[gem] = {
      "gem": htmlReply[URL1_lower:URL1_upper],
      "bar": htmlReply[URL2_lower:URL2_upper]
    }
    
    printf("[%d/%d] %s\n", count, len(gemList), gem)

  return gemMap

data = {}
with open("gemMap.json", "r") as f:
  data = load(f)

for key, item in data.items():
  printf("update data_itemData set icon = \"%s\" where name = \"%s\";\n", item["gem"], key)
  
input("\nPress any key to continue...")
